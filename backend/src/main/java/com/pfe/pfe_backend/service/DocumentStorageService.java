package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Acces bas niveau au stockage objet MinIO (Lot 4, bloc A). Ne connait rien
 * du domaine PFE : elle manipule des cles d'objet (chemins) et des flux
 * d'octets. Le calcul du chemin (arborescence projet/jalon/version) et le
 * lien avec l'entite Document sont geres par DocumentService (4.3).
 */
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /** Cree le bucket au demarrage s'il n'existe pas encore. */
    @PostConstruct
    void initialiserBucket() {
        try {
            boolean existe = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket MinIO '{}' cree", bucket);
            }
        } catch (Exception e) {
            throw erreurStockage("Impossible d'initialiser le bucket MinIO '" + bucket + "'", e);
        }
    }

    /** Depose un objet dans le bucket, en ecrasant tout objet existant a la meme cle. */
    public void uploaderObjet(String cheminObjet, InputStream contenu, long taille, String typeContenu) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cheminObjet)
                    .stream(contenu, taille, -1L)
                    .contentType(typeContenu)
                    .build());
        } catch (Exception e) {
            throw erreurStockage("Echec de l'upload vers MinIO (" + cheminObjet + ")", e);
        }
    }

    /** Recupere le flux de contenu d'un objet. A l'appelant de fermer le flux. */
    public InputStream telechargerObjet(String cheminObjet) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(cheminObjet)
                    .build());
        } catch (Exception e) {
            throw erreurStockage("Echec du telechargement depuis MinIO (" + cheminObjet + ")", e);
        }
    }

    /** Supprime definitivement un objet du bucket (utilise uniquement lors d'un nettoyage physique). */
    public void supprimerObjet(String cheminObjet) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(cheminObjet)
                    .build());
        } catch (Exception e) {
            throw erreurStockage("Echec de la suppression MinIO (" + cheminObjet + ")", e);
        }
    }

    private BusinessException erreurStockage(String message, Exception cause) {
        log.error(message, cause);
        return new BusinessException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
