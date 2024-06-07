package ai.ftech.fekyc.domain.model.ekyc

import android.util.Log
import java.io.Serializable

class EkycFormInfo : Serializable {
    var id: Int? = null
    var cardAttributeId: Int? = null
    var fieldName: String? = null
    var fieldValue: String? = null
    var type: String? = null
    var isEditable: Boolean = false
    var placeholder: String? = null
    var fieldType: FIELD_TYPE? = null
    var dateType: DATE_TYPE? = null

    enum class FIELD_TYPE(val type: String) {
        STRING("string"),
        NUMBER("number"),
        DATE("date"),
        COUNTRY("country"),
        GENDER("gender"),
        NATIONAL("national");

        companion object {
            fun valueOfName(value: String): FIELD_TYPE? {
                val item = values().find {
                    it.type == value
                }

                return item
            }
        }
    }

    enum class DATE_TYPE(val type: Int?) {
        NORMAL(0),
        PASS(1),
        FEATURE(2);

        companion object {
            fun valueOfName(value: Int?): DATE_TYPE? {
                val item = values().find {
                    it.type == value
                }

                return item
            }
        }
    }

    enum class GENDER(val value: String) {
        UNKNOWN("UNKNOWN"),
        MALE("Nam"),
        FEMALE("Nữ");

        companion object {
            fun valueOfName(value: String): GENDER? {
                val item = values().find {
                    it.value == value
                }

                return item
            }
        }
    }
}
