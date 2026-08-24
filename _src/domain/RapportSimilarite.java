package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.NiveauSimilarite;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultat d'une analyse de similarite d'un document contre le corpus
 * archive (Lot 6, etape 6.4).
 *
 * Un rapport est un cliche date : il conserve non seulement le score obtenu,
 * mais aussi les seuils en vigueur au moment de l'analyse et la taille du
 * corpus compare. Sans cela, un rapport consulte six mois plus tard serait
 * ininterpretable : on ne saurait pas si un document non signale l'a ete
 * parce qu'il etait original, ou parce que le corpus etait alors vide et les
 * seuils differents.
 *
 * Plusieurs rapports peuvent coexister pour un meme document : relancer une
 * analyse apres enrichissement du corpus produit une nouvelle ligne plutot
 * que d'ecraser la precedente, ce qui preserve l'historique des decisions.
 */
@Entity
@Table(
        name = "rapport_similarite",
        indexes = {
                @Index(name = "idx_rapport_similarite_document", columnList = "document_id"),
                @Index(name = "idx_rapport_similarite_niveau", columnList = "niveau")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportSimilarite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Document analyse. Ce n'est pas une archive : c'est le rapport depose qu'on verifie. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rapport_similarite_document"))
    private Document document;

    /** Auteur de la demande d'analyse, pour la tracabilite. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "demande_par_id",
            foreignKey = @ForeignKey(name = "fk_rapport_similarite_demandeur"))
    private Utilisateur demandePar;

    /** Meilleur score observe, toutes correspondances confondues. C'est lui qui determine le niveau. */
    @Column(name = "score_max", nullable = false)
    private double scoreMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false, length = 20)
    private NiveauSimilarite niveau;

    /** Nombre de documents archives effectivement compares : un corpus vide rend le rapport non concluant. */
    @Min(0)
    @Column(name = "nb_documents_compares", nullable = false)
    private int nbDocumentsCompares;

    /** Nombre de morceaux du document analyse. */
    @Min(0)
    @Column(name = "nb_morceaux_analyses", nullable = false)
    private int nbMorceauxAnalyses;

    /** Seuils en vigueur lors de l'analyse, figes pour garder le rapport interpretable. */
    @Column(name = "seuil_suspect", nullable = false)
    private double seuilSuspect;

    @Column(name = "seuil_attention", nullable = false)
    private double seuilAttention;

    @Column(name = "date_analyse", nullable = false, updatable = false)
    private LocalDateTime dateAnalyse;

    /**
     * Correspondances retenues, de la plus forte a la plus faible.
     * Cascade complete : un rapport supprime emporte ses correspondances,
     * qui n'ont aucun sens isolement.
     */
    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("score DESC")
    @Builder.Default
    private List<CorrespondanceSimilarite> correspondances = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.dateAnalyse = LocalDateTime.now();
    }

    public void ajouterCorrespondance(CorrespondanceSimilarite correspondance) {
        correspondance.setRapport(this);
        this.correspondances.add(correspondance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RapportSimilarite other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
