package S10P22D204.stream.common.exception;


import lombok.Getter;

@Getter
public enum ExceptionType {

    NOT_VALID_TOKEN(401, "토큰이 유효하지 않습니다."),
    SERVER_ERROR(500, "서버 오류가 발생했습니다."),
    CLIENT_ERROR(400, "잘못된 요청입니다."),
    NOT_VALID_USER(400, "등록된 사용자가 유효하지 않습니다."),
    USER_NOT_FOUND(401, "등록된 사용자가 없습니다."),
    PLAN_ID_MISSING(401, "PlanID가 존재하지 않습니다."),
    MESSAGE_SEND_FAILED(500, "메세지 전송에 실패하였습니다."),
    DATABASE_ERROR(500, "Database Error가 발생하였습니다")
    ;

    private final int code;
    private final String msg;

    ExceptionType (int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
