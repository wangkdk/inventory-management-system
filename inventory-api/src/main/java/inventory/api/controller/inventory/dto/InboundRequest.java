package inventory.api.controller.inventory.dto;

import inventory.domain.stock.InboundItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InboundRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 200) String name,
        @NotNull @Positive Integer quantity
) {

    public InboundItem toInboundItem() {
        return new InboundItem(sku, name, quantity);
    }
}
