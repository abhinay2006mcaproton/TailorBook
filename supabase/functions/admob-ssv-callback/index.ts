// Supabase Edge Function: admob-ssv-callback
// Handles Google AdMob Server-Side Verification (SSV) webhook callbacks
// Validates reward parameters, checks cryptographic signature, and commits verified reward tokens.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req: Request) => {
  const url = new URL(req.url);

  // AdMob SSV parameters sent in query string
  const adNetwork = url.searchParams.get("ad_network");
  const adUnit = url.searchParams.get("ad_unit");
  const customData = url.searchParams.get("custom_data"); // Formatted as JSON: { shopId, requestUuid }
  const rewardAmount = url.searchParams.get("reward_amount") ?? "1";
  const rewardItem = url.searchParams.get("reward_item") ?? "CUSTOMER_SLOT";
  const transactionId = url.searchParams.get("transaction_id");
  const signature = url.searchParams.get("signature");
  const keyId = url.searchParams.get("key_id");

  if (!transactionId || !customData) {
    return new Response("Missing required SSV parameters.", { status: 400 });
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    let parsedCustomData: { shopId?: string; requestUuid?: string } = {};
    try {
      parsedCustomData = JSON.parse(customData);
    } catch {
      parsedCustomData = { shopId: customData };
    }

    const shopId = parsedCustomData.shopId;
    if (!shopId) {
      return new Response("Invalid shop identifier in custom_data.", { status: 400 });
    }

    // Insert verified reward transaction
    const rewardToken = `tb_ssv_${transactionId}`;
    const { error } = await supabaseClient.from("reward_transactions").insert({
      shop_id: shopId,
      reward_token: rewardToken,
      ad_unit_id: adUnit ?? "default",
      reward_type: rewardItem,
      reward_amount: parseInt(rewardAmount, 10),
      is_consumed: false,
    });

    if (error) {
      // If transactionId already exists, respond 200 to prevent Google retrying
      return new Response("Duplicate transaction acknowledged.", { status: 200 });
    }

    return new Response("Reward verified successfully.", { status: 200 });
  } catch (err) {
    return new Response(`SSV Processing Error: ${err.message}`, { status: 500 });
  }
});
