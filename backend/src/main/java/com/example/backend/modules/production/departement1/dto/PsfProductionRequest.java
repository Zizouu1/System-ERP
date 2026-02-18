import com.example.backend.modules.production.shared.entity.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@Builder.Default
@NoArgsConstructor
@AllArgsConstructor
public class PsfProductionRequest {
    private String reference;
    private double totalProducedQuantity;
    private double quantityPerBatch;
    private ProductType productType;
    private boolean producedByCutMachine;
    private String operatorMatricule;
    private LocalTime startTime;
    private LocalTime endTime;
    private int scrapQuantity = 0;
}
