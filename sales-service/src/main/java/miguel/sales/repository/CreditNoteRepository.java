package miguel.sales.repository;

import miguel.sales.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    Optional<CreditNote> findBySaleId(Long saleId);
    Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);
}
