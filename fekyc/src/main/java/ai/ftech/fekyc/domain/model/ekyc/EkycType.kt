package ai.ftech.fekyc.domain.model.ekyc

enum class PHOTO_TYPE {
    SSN,
    DRIVER_LICENSE,
    PASSPORT
}

enum class PHOTO_INFORMATION(var value: String) {
    FRONT("FRONT"),
    BACK("BACK"),
    FACE(""),
    PAGE_NUMBER_2("")
}

enum class UPLOAD_STATUS {
    NONE,
    SUCCESS,
    COMPLETE,
    FAIL
}

enum class CAPTURE_TYPE(var value: String?) {
    FRONT("front"),
    BACK("back"),
    FACE(null),
}

enum class FACE_POSE(var value: Float) {
    LEFT(30f),
    RIGHT(-30f),
    UP(16f),
    DOWN(-14f),
    STRAIGHT(12f)
}

enum class FACE_POSE_TYPE(var value: String) {
    LEFT("left"),
    RIGHT("right"),
    UP("top"),
    DOWN("down"),
    NONE("none")
}

enum class DEVICE_TYPE(var value: String) {
    ANDROID("Android"),
    IOS("iOS")
}

enum class CROP_TYPE {
    DEFAULT,
    FULL_SCREEN,
    LARGE,
}



