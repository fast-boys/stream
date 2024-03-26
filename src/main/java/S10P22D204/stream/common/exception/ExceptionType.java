package S10P22D204.stream.common.exception;


import lombok.Getter;

@Getter
public enum ExceptionType {

    NOT_VALID_TOKEN(401, "토큰이 유효하지 않습니다."),
    SERVER_ERROR(500, "서버 오류가 발생했습니다."),
    CLIENT_ERROR(400, "잘못된 요청입니다."),
    ;

    private final int code;
    private final String msg;

    ExceptionType (int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
