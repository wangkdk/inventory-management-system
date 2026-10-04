package inventory.domain.stock;

public record InboundResult(StockStatus status, boolean newlyRegistered) {
}
