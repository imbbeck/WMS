//package com.wms.applicationInfra.exception;
//
//import com.wms.location.domain.exception.LocationException;
////import com.wms.logisticTask.domain.exception.LogisticTaskException;
////import com.wms.logisticTemplate.domain.exception.LogisticTemplateException;
////import com.wms.stock.domain.exception.StockException;
////import com.wms.userInfo.domain.exception.UserInfoException;
////import com.wms.ware.domain.exception.WareException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.context.request.WebRequest;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    // Validation 예외를 400 에러로 처리
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
//        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
//                .map(error -> ErrorResponse.FieldError.builder()
//                        .field(error.getField())
//                        .value(error.getRejectedValue() != null ? error.getRejectedValue().toString() : null)
//                        .reason(error.getDefaultMessage())
//                        .build())
//                .collect(Collectors.toList());
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .message("입력값이 올바르지 않습니다.")
//                .errorCode(ErrorResponse.ErrorCodes.VALIDATION_ERROR)
//                .status(HttpStatus.BAD_REQUEST.value())
//                .timestamp(java.time.LocalDateTime.now())
//                .path(request.getDescription(false).replace("uri=", ""))
//                .fieldErrors(fieldErrors)
//                .build();
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(errorResponse);
//    }
//
////    // UserInfo 도메인 예외 처리
////    @ExceptionHandler(UserInfoException.class)
////    public ResponseEntity<ErrorResponse> handleUserInfoException(UserInfoException ex, WebRequest request) {
////        log.error("UserInfo error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.USER_NOT_FOUND,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(UserInfoException.NotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleUserInfoNotFoundException(UserInfoException.NotFoundException ex, WebRequest request) {
////        log.error("UserInfo not found error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.USER_NOT_FOUND,
////                HttpStatus.NOT_FOUND.value(), request);
////        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
////    }
////
////    @ExceptionHandler(UserInfoException.DuplicateUsernameException.class)
////    public ResponseEntity<ErrorResponse> handleDuplicateUsernameException(UserInfoException.DuplicateUsernameException ex, WebRequest request) {
////        log.error("Duplicate username error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.USER_DUPLICATE_USERNAME,
////                HttpStatus.CONFLICT.value(), request);
////        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
////    }
////
////    @ExceptionHandler(UserInfoException.DuplicateEmailException.class)
////    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(UserInfoException.DuplicateEmailException ex, WebRequest request) {
////        log.error("Duplicate email error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.USER_DUPLICATE_EMAIL,
////                HttpStatus.CONFLICT.value(), request);
////        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
////    }
////
////    @ExceptionHandler(UserInfoException.InvalidUsernameException.class)
////    public ResponseEntity<ErrorResponse> handleInvalidUsernameException(UserInfoException.InvalidUsernameException ex, WebRequest request) {
////        log.error("Invalid username error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.USER_INVALID_USERNAME,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
//    }
//
////    @ExceptionHandler(UserInfoException.WrongPasswordException.class)
////    public ResponseEntity<ErrorResponse> handleWrongPasswordException(UserInfoException.WrongPasswordException ex, WebRequest request) {
////        log.error("Wrong password error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.USER_WRONG_PASSWORD,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(UserInfoException.UnauthorizedWithdrawException.class)
////    public ResponseEntity<ErrorResponse> handleUnauthorizedWithdrawException(UserInfoException.UnauthorizedWithdrawException ex, WebRequest request) {
////        log.error("Unauthorized withdraw error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.USER_UNAUTHORIZED_WITHDRAW,
////                HttpStatus.FORBIDDEN.value(), request);
////        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
////    }
////
////    // Ware 도메인 예외 처리
////    @ExceptionHandler(WareException.class)
////    public ResponseEntity<ErrorResponse> handleWareException(WareException ex, WebRequest request) {
////        log.error("Ware error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.WARE_INVALID_NAME,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(WareException.NotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleWareNotFoundException(WareException.NotFoundException ex, WebRequest request) {
////        log.error("Ware not found error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.WARE_NOT_FOUND,
////                HttpStatus.NOT_FOUND.value(), request);
////        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
////    }
////
////    @ExceptionHandler(WareException.DuplicateException.class)
////    public ResponseEntity<ErrorResponse> handleWareDuplicateNameException(WareException.DuplicateException ex, WebRequest request) {
////        log.error("Ware duplicate name error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.WARE_DUPLICATE_NAME,
////                HttpStatus.CONFLICT.value(), request);
////        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
////    }
////
////    @ExceptionHandler(WareException.InvalidNameException.class)
////    public ResponseEntity<ErrorResponse> handleWareInvalidNameException(WareException.InvalidNameException ex, WebRequest request) {
////        log.error("Ware invalid name error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.WARE_INVALID_NAME,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(WareException.InvalidTypeException.class)
////    public ResponseEntity<ErrorResponse> handleWareInvalidTypeException(WareException.InvalidTypeException ex, WebRequest request) {
////        log.error("Ware invalid type error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.WARE_INVALID_TYPE,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(WareException.InvalidPaletteUnitException.class)
////    public ResponseEntity<ErrorResponse> handleWareInvalidPaletteUnitException(WareException.InvalidPaletteUnitException ex, WebRequest request) {
////        log.error("Ware invalid palette unit error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.WARE_INVALID_PALETTE_UNIT,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    // LogisticTemplate 도메인 예외 처리
////    @ExceptionHandler(LogisticTemplateException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateException(LogisticTemplateException ex, WebRequest request) {
////        log.error("LogisticTemplate error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_INVALID_NAME,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTemplateException.NotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateNotFoundException(LogisticTemplateException.NotFoundException ex, WebRequest request) {
////        log.error("LogisticTemplate not found error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_NOT_FOUND,
////                HttpStatus.NOT_FOUND.value(), request);
////        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTemplateException.InvalidNameException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateInvalidNameException(LogisticTemplateException.InvalidNameException ex, WebRequest request) {
////        log.error("LogisticTemplate invalid name error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_INVALID_NAME,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTemplateException.InvalidTypeException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateInvalidTypeException(LogisticTemplateException.InvalidTypeException ex, WebRequest request) {
////        log.error("LogisticTemplate invalid type error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_INVALID_TYPE,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTemplateException.InvalidStandardQuantityException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateInvalidQuantityException(LogisticTemplateException.InvalidStandardQuantityException ex, WebRequest request) {
////        log.error("LogisticTemplate invalid quantity error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_INVALID_QUANTITY,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTemplateException.WareNotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateWareNotFoundException(LogisticTemplateException.WareNotFoundException ex, WebRequest request) {
////        log.error("LogisticTemplate ware not found error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_WARE_NOT_FOUND,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTemplateException.FromLocationNotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateFromLocationNotFoundException(LogisticTemplateException.FromLocationNotFoundException ex, WebRequest request) {
////        log.error("LogisticTemplate from location not found error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_LOCATION_NOT_FOUND,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTemplateException.ToLocationNotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTemplateToLocationNotFoundException(LogisticTemplateException.ToLocationNotFoundException ex, WebRequest request) {
////        log.error("LogisticTemplate to location not found error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TEMPLATE_LOCATION_NOT_FOUND,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    // LogisticTask 도메인 예외 처리
////    @ExceptionHandler(LogisticTaskException.class)
////    public ResponseEntity<ErrorResponse> handleLogisticTaskException(LogisticTaskException ex, WebRequest request) {
////        log.error("LogisticTask error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TASK_INVALID_DATA,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTaskException.TaskNotModifiableException.class)
////    public ResponseEntity<ErrorResponse> handleTaskNotModifiableException(LogisticTaskException.TaskNotModifiableException ex, WebRequest request) {
////        log.error("Task not modifiable error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TASK_NOT_MODIFIABLE,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTaskException.TaskCancellationNotAllowedException.class)
////    public ResponseEntity<ErrorResponse> handleTaskCancellationNotAllowedException(LogisticTaskException.TaskCancellationNotAllowedException ex, WebRequest request) {
////        log.error("Task cancellation not allowed error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TASK_CANCELLATION_NOT_ALLOWED,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(LogisticTaskException.TaskFailureNotAllowedException.class)
////    public ResponseEntity<ErrorResponse> handleTaskFailureNotAllowedException(LogisticTaskException.TaskFailureNotAllowedException ex, WebRequest request) {
////        log.error("Task failure not allowed error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.TASK_FAILURE_NOT_ALLOWED,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    // Location 도메인 예외 처리
////    @ExceptionHandler(LocationException.NotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleLocationNotFoundException(LocationException.NotFoundException ex, WebRequest request) {
////        log.error("Location not found error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.LOCATION_NOT_FOUND,
////                HttpStatus.NOT_FOUND.value(), request);
////        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
////    }
////
////    @ExceptionHandler(LocationException.DuplicateException.class)
////    public ResponseEntity<ErrorResponse> handleLocationDuplicateNameException(LocationException.DuplicateException ex, WebRequest request) {
////        log.error("Location duplicate name error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.LOCATION_DUPLICATE_NAME,
////                HttpStatus.CONFLICT.value(), request);
////        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
////    }
////
////    @ExceptionHandler(LocationException.class)
////    public ResponseEntity<ErrorResponse> handleLocationException(LocationException ex, WebRequest request) {
////        log.error("Location error occurred: ", ex);
////        ErrorResponse errorResponse = createErrorResponse(ex.getMessage(), ErrorResponse.ErrorCodes.LOCATION_INVALID_TYPE,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    // Stock 도메인 예외 처리
////    @ExceptionHandler(StockException.class)
////    public ResponseEntity<ErrorResponse> handleStockException(StockException e, WebRequest request) {
////        log.error("Stock error occurred: ", e);
////        ErrorResponse errorResponse = createErrorResponse(e.getMessage(), ErrorResponse.ErrorCodes.STOCK_INVALID_QUANTITY,
////                HttpStatus.BAD_REQUEST.value(), request);
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////    }
////
////    @ExceptionHandler(StockException.InsufficientStockException.class)
////    public ResponseEntity<ErrorResponse> handleInsufficientStockException(StockException.InsufficientStockException e, WebRequest request) {
////        log.error("Insufficient stock error occurred: ", e);
////        ErrorResponse errorResponse = createErrorResponse(e.getMessage(), ErrorResponse.ErrorCodes.STOCK_INSUFFICIENT,
////                HttpStatus.CONFLICT.value(), request);
////        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
////    }
////
////    @ExceptionHandler(StockException.WarehouseCapacityExceededException.class)
////    public ResponseEntity<ErrorResponse> handleWarehouseCapacityExceededException(StockException.WarehouseCapacityExceededException e, WebRequest request) {
////        log.error("Warehouse capacity exceeded error occurred: ", e);
////        ErrorResponse errorResponse = createErrorResponse(e.getMessage(), ErrorResponse.ErrorCodes.STOCK_CAPACITY_EXCEEDED,
////                HttpStatus.CONFLICT.value(), request);
////        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
////    }
////
////    @ExceptionHandler(StockException.NotFoundException.class)
////    public ResponseEntity<ErrorResponse> handleStockNotFoundException(StockException.NotFoundException e, WebRequest request) {
////        log.error("Stock not found error occurred: ", e);
////        ErrorResponse errorResponse = createErrorResponse(e.getMessage(), ErrorResponse.ErrorCodes.STOCK_NOT_FOUND,
////                HttpStatus.NOT_FOUND.value(), request);
////        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
////    }
//
//    // 일반적인 IllegalArgumentException 처리
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
//        log.error("Invalid argument error occurred: ", e);
//        ErrorResponse errorResponse = createErrorResponse(e.getMessage(), ErrorResponse.ErrorCodes.VALIDATION_ERROR,
//                HttpStatus.BAD_REQUEST.value(), request);
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
//    }
//
//    // 예상치 못한 예외 처리
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleException(Exception e, WebRequest request) {
//        log.error("Unexpected error occurred: ", e);
//        ErrorResponse errorResponse = createErrorResponse("서버 내부 오류가 발생했습니다.", ErrorResponse.ErrorCodes.INTERNAL_SERVER_ERROR,
//                HttpStatus.INTERNAL_SERVER_ERROR.value(), request);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//    }
//
//    /**
//     * 공통 에러 응답 생성 메서드
//     */
//    private ErrorResponse createErrorResponse(String message, String errorCode, int status, WebRequest request) {
//        return ErrorResponse.builder()
//                .message(message)
//                .errorCode(errorCode)
//                .status(status)
//                .timestamp(java.time.LocalDateTime.now())
//                .path(request.getDescription(false).replace("uri=", ""))
//                .build();
//    }
//}