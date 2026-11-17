package com.tailorbook.feature.measurements.domain.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import com.tailorbook.R

/**
 * Single measurement point definition for a garment template.
 */
data class MeasurementField(
    val key: String,
    val nameEn: String,
    val nameTe: String,
    val hint: String = "",
    val defaultVal: Double = 0.0,
    val minVal: Double = 2.0,
    val maxVal: Double = 70.0
)

/**
 * Garment Template definition (TASK-06-01 / REQ-MEAS-01).
 * Contains industry-standard measurement points for Indian boutiques and tailoring shops.
 */
data class GarmentTemplate(
    val id: String,
    val category: GarmentCategory,
    @StringRes val titleRes: Int = 0,
    val customNameEn: String? = null,
    val customNameTe: String? = null,
    val fields: List<MeasurementField>,
    val icon: ImageVector = Icons.Default.Checkroom,
    val isCustom: Boolean = false
) {
    fun getDisplayName(): String {
        return customNameEn ?: id.lowercase().replaceFirstChar { it.uppercase() }
    }
}

enum class GarmentCategory {
    WOMEN,
    MEN,
    UNISEX
}

object GarmentTemplates {

    val BLOUSE = GarmentTemplate(
        id = "BLOUSE",
        category = GarmentCategory.WOMEN,
        titleRes = R.string.garment_blouse,
        icon = Icons.Default.Checkroom,
        fields = listOf(
            MeasurementField("shoulder", "Shoulder", "భుజం", "Shoulder bone to shoulder bone", 14.0),
            MeasurementField("length", "Length", "పొడవు", "Shoulder to waist / hem length", 14.0),
            MeasurementField("chest", "Bust / Chest", "ఛాతీ", "Around fullest part of bust", 36.0),
            MeasurementField("upper_chest", "Upper Chest", "పై ఛాతీ", "Above bust around armpits", 34.0),
            MeasurementField("waist", "Waist", "నడుము", "Around natural waist line", 30.0),
            MeasurementField("front_neck", "Front Neck", "ముందు మెడ", "Front neck depth", 6.5),
            MeasurementField("back_neck", "Back Neck", "వెనుక మెడ", "Back neck depth", 8.0),
            MeasurementField("armhole", "Armhole", "చంక చుట్టు", "Circumference around arm socket", 16.0),
            MeasurementField("sleeve_length", "Sleeve Length", "చేయి పొడవు", "Shoulder seam to sleeve edge", 10.0),
            MeasurementField("sleeve_round", "Sleeve Round", "చేయి లూజు", "Around bicep / sleeve hem", 12.0),
            MeasurementField("cross_front", "Cross Front", "ముందు క్రాస్", "Across front armpit to armpit", 13.0),
            MeasurementField("cross_back", "Cross Back", "వెనుక క్రాస్", "Across back shoulder blades", 14.0),
            MeasurementField("apex_length", "Apex / Point Length", "డాట్ పొడవు", "Shoulder to bust point", 10.0),
            MeasurementField("apex_width", "Apex Width", "డాట్ల వెడల్పు", "Distance between bust apex points", 7.0)
        )
    )

    val KURTI = GarmentTemplate(
        id = "KURTI",
        category = GarmentCategory.WOMEN,
        titleRes = R.string.garment_kurti,
        icon = Icons.Default.DryCleaning,
        fields = listOf(
            MeasurementField("shoulder", "Shoulder", "భుజం", "Across shoulders", 14.5),
            MeasurementField("length", "Length", "పొడవు", "Shoulder down to desired hem", 40.0),
            MeasurementField("bust", "Bust", "ఛాతీ", "Around fullest part of bust", 36.0),
            MeasurementField("waist", "Waist", "నడుము", "Around natural waist", 32.0),
            MeasurementField("hip", "Hip", "సీటు లూజు", "Around fullest part of hip", 38.0),
            MeasurementField("armhole", "Armhole", "చంక చుట్టు", "Around armhole", 16.5),
            MeasurementField("sleeve_length", "Sleeve Length", "చేయి పొడవు", "Shoulder to cuff", 16.0),
            MeasurementField("sleeve_round", "Sleeve Round", "చేయి లూజు", "Around forearm / wrist", 10.0),
            MeasurementField("front_neck", "Front Neck", "ముందు మెడ", "Front neck drop", 6.5),
            MeasurementField("back_neck", "Back Neck", "వెనుక మెడ", "Back neck drop", 5.0),
            MeasurementField("slit_length", "Side Slit Length", "కట్స్ పొడవు", "Waist to slit start", 14.0)
        )
    )

    val SALWAR = GarmentTemplate(
        id = "SALWAR",
        category = GarmentCategory.WOMEN,
        titleRes = R.string.garment_churidar,
        icon = Icons.Default.AccessibilityNew,
        fields = listOf(
            MeasurementField("length", "Full Length", "పూర్తి పొడవు", "Waist to ankle", 38.0),
            MeasurementField("waist_belt", "Waist / Belt", "నడుము / బెల్ట్", "Waist circumference with belt", 34.0),
            MeasurementField("hip", "Hip", "సీటు లూజు", "Around widest part of hips", 40.0),
            MeasurementField("thigh", "Thigh Round", "తొడ లూజు", "Widest part of upper thigh", 22.0),
            MeasurementField("knee", "Knee Round", "మోకాలు", "Around knee", 16.0),
            MeasurementField("calf", "Calf Round", "పిక్క లూజు", "Around calf muscle", 14.0),
            MeasurementField("ankle_opening", "Ankle / Bottom", "బాటమ్ లూజు", "Ankle opening circumference", 12.0)
        )
    )

    val DRESS = GarmentTemplate(
        id = "DRESS",
        category = GarmentCategory.WOMEN,
        titleRes = R.string.garment_dress,
        icon = Icons.Default.Palette,
        fields = listOf(
            MeasurementField("shoulder", "Shoulder", "భుజం", "Shoulder width", 14.0),
            MeasurementField("full_length", "Full Length", "పూర్తి పొడవు", "Shoulder to bottom hem", 52.0),
            MeasurementField("upper_body", "Body Length", "బాడీ పొడవు", "Shoulder to waist join", 15.0),
            MeasurementField("bust", "Bust", "ఛాతీ", "Fullest chest circumference", 36.0),
            MeasurementField("waist", "Waist", "నడుము", "Waist circumference", 30.0),
            MeasurementField("hip", "Hip", "సీటు లూజు", "Hip circumference", 38.0),
            MeasurementField("gher_flare", "Flare / Gher", "ఘేరా", "Total bottom circle flare", 120.0),
            MeasurementField("sleeve_length", "Sleeve Length", "చేయి పొడవు", "Sleeve length", 18.0)
        )
    )

    val SHIRT = GarmentTemplate(
        id = "SHIRT",
        category = GarmentCategory.MEN,
        titleRes = R.string.garment_shirt,
        icon = Icons.Default.Person,
        fields = listOf(
            MeasurementField("shoulder", "Shoulder", "భుజం", "Shoulder point to point", 18.0),
            MeasurementField("length", "Shirt Length", "పొడవు", "Collar base to bottom hem", 29.0),
            MeasurementField("chest", "Chest", "ఛాతీ", "Chest circumference", 40.0),
            MeasurementField("waist", "Waist", "నడుము", "Stomach / waist circumference", 36.0),
            MeasurementField("hip", "Hip", "సీటు", "Bottom hem hip width", 40.0),
            MeasurementField("sleeve_length", "Sleeve Length", "చేయి పొడవు", "Shoulder to wrist cuff", 24.5),
            MeasurementField("collar", "Collar / Neck", "కాలర్", "Neck circumference", 15.5),
            MeasurementField("cuff", "Cuff Round", "కఫ్", "Wrist cuff circumference", 9.5),
            MeasurementField("bicep", "Bicep Round", "బైసెప్", "Around upper arm", 14.0)
        )
    )

    val TROUSER = GarmentTemplate(
        id = "TROUSER",
        category = GarmentCategory.MEN,
        titleRes = R.string.garment_pants,
        icon = Icons.Default.Straighten,
        fields = listOf(
            MeasurementField("length", "Outseam Length", "పొడవు", "Waistband to shoe top", 40.0),
            MeasurementField("waist", "Waist", "నడుము", "Where trouser waist sits", 34.0),
            MeasurementField("hip", "Hip", "సీటు", "Fullest seat circumference", 40.0),
            MeasurementField("inseam", "Inseam Length", "లోపలి పొడవు", "Crotch to bottom hem", 30.0),
            MeasurementField("thigh", "Thigh Round", "తొడ లూజు", "Upper thigh circumference", 24.0),
            MeasurementField("knee", "Knee Round", "మోకాలు", "Around knee", 18.0),
            MeasurementField("bottom", "Bottom / Hem", "బాటమ్", "Trouser leg opening", 15.0),
            MeasurementField("rise", "Rise / Fork", "కిస్తా", "Crotch depth", 11.5)
        )
    )

    val TSHIRT = GarmentTemplate(
        id = "TSHIRT",
        category = GarmentCategory.UNISEX,
        titleRes = R.string.garment_tshirt,
        icon = Icons.Default.ContentCut,
        fields = listOf(
            MeasurementField("shoulder", "Shoulder", "భుజం", "Across back shoulders", 17.5),
            MeasurementField("length", "Length", "పొడవు", "Shoulder to hem", 28.0),
            MeasurementField("chest", "Chest", "ఛాతీ", "Chest circumference", 38.0),
            MeasurementField("sleeve_length", "Sleeve Length", "చేయి పొడవు", "Shoulder to sleeve hem", 8.5),
            MeasurementField("neck", "Neck Opening", "మెడ", "Neckband circumference", 16.0)
        )
    )

    val CUSTOM = GarmentTemplate(
        id = "CUSTOM",
        category = GarmentCategory.UNISEX,
        titleRes = R.string.garment_custom,
        icon = Icons.Default.Straighten,
        fields = listOf(
            MeasurementField("shoulder", "Shoulder", "భుజం", "Shoulder measurement", 15.0),
            MeasurementField("length", "Full Length", "పూర్తి పొడవు", "Primary garment length", 36.0),
            MeasurementField("chest_bust", "Chest / Bust", "ఛాతీ", "Primary width measurement", 36.0),
            MeasurementField("waist", "Waist", "నడుము", "Waist circumference", 32.0),
            MeasurementField("hip", "Hip", "సీటు", "Hip circumference", 38.0),
            MeasurementField("sleeve", "Sleeve", "చేయి పొడవు", "Sleeve length", 12.0)
        )
    )

    private val dynamicTemplates = mutableListOf<GarmentTemplate>()

    fun registerTemplates(templates: List<GarmentTemplate>) {
        dynamicTemplates.clear()
        dynamicTemplates.addAll(templates)
    }

    val ALL_TEMPLATES: List<GarmentTemplate>
        get() {
            val builtIn = listOf(
                BLOUSE,
                KURTI,
                SALWAR,
                DRESS,
                SHIRT,
                TROUSER,
                TSHIRT,
                CUSTOM
            )
            val dynamicFiltered = dynamicTemplates.filter { dt -> builtIn.none { it.id.equals(dt.id, ignoreCase = true) } }
            return builtIn + dynamicFiltered
        }

    fun findById(id: String): GarmentTemplate {
        return ALL_TEMPLATES.find { it.id.equals(id, ignoreCase = true) } ?: BLOUSE
    }
}
