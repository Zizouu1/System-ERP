import com.example.backend.modules.production.shared.entity.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PsfProductionRequest {
    private String reference;
    private double totalProducedQuantity;
    private double quantityPerBatch;
    private ProductType productType;
    private boolean producedByCutMachine;
    private String operatorMatricule;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
}
