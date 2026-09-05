package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.StatutIndexationArticle;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Article de documentation interne / FAQ, corpus de l'assistant
 * conversationnel RAG (Lot 8, EF-48, cahier des charges section 7.1 et
 * chapitre 7 ; diagramme de sequence 6). Le contenu n'existe pas encore a
 * cette etape : il sera redige puis valide avant indexation (etape 8.5).
 *
 * Contrairement au corpus du lot 6 (rapports PDF de plusieurs dizaines de
 * pages, decoupes par DecoupageTexteService en MorceauDocument), un article
 * d'aide est court par construction : un vecteur par article suffit, sans
 * table de "morceaux" separee. Contrainte assumee : un article qui
 * depasserait la fenetre de gemini-embedding-2 (8192 tokens) devrait etre
 * scinde en plusieurs articles plutot que decoupe automatiquement - aucun
 * decoupage automatique n'est prevu.
 */
@Entity
@Table(
        name = "article_aide",
        indexes = {
                @Index(name = "idx_article_aide_categorie", columnList = "categorie"),
                @Index(name = "idx_article_aide_statut_indexation", columnList = "statut_indexation")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleAide {

    /** Dimension du vecteur. Doit rester alignee sur gemini.embeddings.dimension. */
    public static final int DIMENSION = 768;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Question ou intitule de l'article (ex. "Comment deposer mon rapport
     * final ?"). Sert aussi de libelle de source cite par l'assistant
     * (etape 8.7/8.11).
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "titre", nullable = false, length = 255)
    private String titre;

    /** Reponse / contenu documentaire, vectorise tel quel (voir Javadoc de la classe). */
    @NotBlank
    @Column(name = "contenu", columnDefinition = "TEXT", nullable = false)
    private String contenu;

    /** Regroupement thematique (ex. "Depot de documents", "Jalons"), meme role que Ressource.categorie. */
    @NotBlank
    @Size(max = 100)
    @Column(name = "categorie", nullable = false, length = 100)
    private String categorie;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_indexation", nullable = false, length = 30)
    private StatutIndexationArticle statutIndexation = StatutIndexationArticle.EN_ATTENTE;

    /**
     * Vecteur d'embedding, calcule a l'indexation (etape 8.6). Null tant que
     * statutIndexation n'est pas VECTORISE.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = DIMENSION)
    @Column(name = "vecteur", columnDefinition = "vector(768)")
    private float[] vecteur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Derniere modification (contenu ou statut d'indexation) ; horodatage utilitaire d'administration. */
    @Column(name = "date_maj", nullable = false)
    private LocalDateTime dateMaj;

    @PrePersist
    protected void onCreate() {
        LocalDateTime maintenant = LocalDateTime.now();
        this.dateCreation = maintenant;
        this.dateMaj = maintenant;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateMaj = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleAide other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
