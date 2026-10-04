package inventory.api.controller.inventory.dto;

import inventory.domain.stock.OutboundItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OutboundRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotNull @Positive Integer quantity
) {

    public OutboundItem toOutboundItem() {
        return new OutboundItem(sku, quantity);
    }
}
