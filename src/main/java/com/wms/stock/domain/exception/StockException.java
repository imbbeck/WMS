package com.wms.stock.domain.exception;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.applicationInfra.exception.BusinessException;
import com.wms.applicationInfra.exception.DomainExceptionHelper;
import com.wms.location.domain.model.Location;
import com.wms.ware.domain.model.Ware;

public final class StockException {
	private StockException() {}

	// ValidationEx
	public static class ValidationEx extends BusinessException.ValidationException {
		public ValidationEx(String message) {
			super(message);
		}
	}

	public static ValidationEx validation(String field, String additionalMessage) {
		return new ValidationEx(DomainExceptionHelper.validation(field, additionalMessage));
	}

	public static ValidationEx validation(FieldEnum fieldEnum) {
		return new ValidationEx(DomainExceptionHelper.validation(fieldEnum));
	}

	public static ValidationEx validation(String message) {
		return new ValidationEx(message);
	}

	// NotFoundEx
	public static class NotFoundEx extends BusinessException.NotFoundException {
		public NotFoundEx(String message) {
			super(message);
		}
	}

	public static NotFoundEx notFound(Long stockId) {
		return new NotFoundEx(DomainExceptionHelper.notFound(stockId));
	}

	// InsufficientStockEx
	public static class InsufficientStockEx extends BusinessException.ConflictException {
		public InsufficientStockEx(String message) {
			super(message);
		}
	}

	public static InsufficientStockEx insufficientStock(long locationId, long wareId, int requested, int available) {
		return new InsufficientStockEx(String.format("재고 부족: 물품(%s) 위치(%s) 요청 수량(%d) 가용 수량(%d)",
				wareId, locationId, requested, available));
	}

} 