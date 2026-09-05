# Corpus FAQ / documentation interne — Assistant RAG (Lot 8, étape 8.5)

**Statut : brouillon à valider.** Ce document liste les articles proposés pour le corpus de l'assistant conversationnel (EF-48). Rien n'est encore en base — une fois ce contenu validé, il sera chargé dans `ArticleAide` et vectorisé à l'étape 8.6.

Chaque article a trois champs : **catégorie** (regroupement thématique), **titre** (la question — c'est aussi le libellé cité comme source par l'assistant) et **contenu** (la réponse, qui sera vectorisée telle quelle).

Le contenu s'appuie uniquement sur ce qui existe réellement dans la plateforme (lots 0 à 7) : rien n'est inventé ni anticipé sur des fonctionnalités futures. Le corpus vise en priorité les questions d'un étudiant (le widget de chat n'est prévu que sur l'espace étudiant, étape 8.12), avec deux articles de méta-transparence exigés par le cahier des charges (EF-49, ENF-15).

---

## Compte et connexion

### 1. Comment créer mon compte étudiant ?
Sur la page d'inscription, renseignez votre nom, prénom, email et un mot de passe d'au moins 8 caractères. Trois champs supplémentaires sont obligatoires pour un compte étudiant : votre numéro étudiant, votre filière et votre promotion. Une fois le compte créé, connectez-vous avec votre email et votre mot de passe pour accéder à votre espace.

### 2. J'ai oublié mon mot de passe, que faire ?
Il n'existe pas encore de réinitialisation en libre-service sur la plateforme. Contactez l'administrateur : lui seul peut réinitialiser un mot de passe.

### 3. Pourquoi suis-je parfois déconnecté sans prévenir ?
Votre session repose sur un jeton d'accès valable 15 minutes, renouvelé automatiquement en arrière-plan tant que vous naviguez activement sur la plateforme. Une déconnexion inattendue signifie généralement une longue période d'inactivité ou une déconnexion explicite depuis un autre onglet.

---

## Sujet et équipe

### 4. Comment créer une équipe et devenir chef d'équipe ?
Tout étudiant qui n'appartient pas encore à une équipe peut en créer une ; il en devient automatiquement le chef. Le chef peut ensuite ajouter des membres par leur numéro étudiant.

### 5. Comment rejoindre une équipe existante ?
Deux façons : soit le chef de l'équipe vous ajoute directement avec votre numéro étudiant, soit vous rejoignez vous-même une équipe ayant de la place, via le bouton prévu à cet effet — à condition d'appartenir à la même filière et à la même promotion que cette équipe, et qu'elle ne soit pas déjà complète.

### 6. Comment mon équipe obtient-elle un sujet de PFE ?
Les sujets sont proposés par les encadrants puis examinés par l'administrateur (statuts : proposé, en validation, puis validé, rejeté ou à corriger). Une fois un sujet validé, seul le chef de votre équipe peut demander l'affectation de l'équipe à ce sujet — c'est cette affectation qui crée le projet officiel de PFE. Une équipe ne peut avoir qu'un seul projet.

### 7. Puis-je changer de sujet une fois mon équipe affectée à un projet ?
Non : une équipe ne peut avoir qu'un seul projet actif. Si vous rencontrez un problème avec votre sujet, la solution passe par votre encadrant, pas par une nouvelle affectation.

---

## Jalons (checkpoints)

### 8. Comment soumettre un jalon (checkpoint) ?
Seul le chef d'équipe peut soumettre un jalon, depuis l'espace jalons du projet : il fournit un lien vers le livrable correspondant et, s'il le souhaite, un commentaire. Le jalon passe alors en attente de validation par l'encadrant, qui peut le valider ou renvoyer un retour.

### 9. Pourquoi mon jalon apparaît-il « en retard » ?
Ce statut est calculé automatiquement, une fois par jour (tôt le matin), pour tout jalon dont l'échéance est dépassée sans avoir été validé. Il n'y a pas de déclenchement manuel : si vous soumettez un jalon après son échéance, le passage en retard peut prendre jusqu'au lendemain pour se refléter.

---

## Documents

### 10. Comment déposer mon rapport ou un livrable ?
Seul le chef d'équipe peut déposer un document, depuis l'espace documents du projet, en le rattachant éventuellement à un jalon précis. Si vous déposez un fichier portant le même nom qu'un document déjà présent, une nouvelle version est créée automatiquement — l'ancienne reste consultable dans l'historique, rien n'est écrasé.

### 11. Comment consulter les anciennes versions d'un document ?
Depuis la fiche du document dans l'espace documents, l'historique complet des versions déposées sous ce nom est accessible, avec la possibilité de télécharger chacune d'elles.

---

## Messagerie et notifications

### 12. Comment contacter mon encadrant sur la plateforme ?
Chaque projet dispose d'une messagerie privée, accessible en lecture et en écriture uniquement au chef d'équipe et à l'encadrant du projet. Les nouveaux messages apparaissent automatiquement après quelques secondes, sans qu'il soit nécessaire de recharger la page.

### 13. À quoi sert la cloche de notifications ?
Elle vous informe des événements qui vous concernent : décision sur un sujet, jalon soumis ou validé, nouveau document déposé, nouveau message, ou alerte de similarité (pour un encadrant). Le badge affiche le nombre de notifications non lues ; vous pouvez marquer une notification comme lue individuellement, ou tout marquer lu d'un coup.

---

## Bibliothèque de ressources

### 14. Qu'est-ce que la bibliothèque de ressources et qui peut y déposer ?
C'est un espace partagé de ressources utiles (liens externes et/ou fichiers), organisées par catégorie, accessible en consultation à tous les comptes de la plateforme. Le dépôt d'une nouvelle ressource est réservé aux encadrants et à l'administrateur.

---

## Détection de similarité

### 15. Mon rapport est-il vérifié automatiquement contre le plagiat ? Puis-je consulter mon score ?
Les rapports archivés par l'administrateur comme documents de référence sont comparés entre eux par une analyse de similarité sémantique, à l'initiative de l'encadrant. Ce résultat n'est toutefois jamais accessible à l'étudiant : ni le score, ni le rapport détaillé — seuls l'encadrant du projet concerné et l'administrateur peuvent le consulter. Si vous avez une question sur l'originalité attendue de votre travail, adressez-vous directement à votre encadrant.

---

## Assistant IA

### 16. Qui répond dans cet assistant ? Est-ce une intelligence artificielle ?
Oui. Les réponses de cet assistant sont générées automatiquement par un modèle de langage, à partir uniquement des articles de cette documentation interne — jamais à partir de vos données personnelles, qui ne sont pas transmises au modèle. Comme toute production issue de l'intelligence artificielle sur cette plateforme, ces réponses sont explicitement signalées comme telles et doivent être vérifiées avant d'être considérées comme définitives.

### 17. Que se passe-t-il si l'assistant ne trouve pas de réponse à ma question ?
L'assistant ne répond qu'à partir des articles disponibles dans cette documentation. S'il ne trouve aucun passage suffisamment pertinent pour votre question, il l'indique honnêtement plutôt que d'inventer une réponse, et vous propose d'être mis en relation avec votre encadrant.

---

## Notes pour la validation

- 17 articles, 8 catégories. Périmètre volontairement centré sur l'étudiant (seul le widget étudiant est prévu, étape 8.12) ; rien sur les vues encadrant/admin.
- Aucune fonctionnalité inexistante n'est mentionnée (pas de réinitialisation de mot de passe en libre-service, pas de messagerie en temps réel WebSocket, pas de score de similarité visible par l'étudiant, etc.) — vérifié contre le code actuel du backend, pas seulement contre les docs de suivi.
- Les articles 16 et 17 répondent aux exigences EF-49 (signalement de toute production IA) et ENF-15 (aucune donnée nominative transmise) : à ne pas retirer ni affaiblir.
- Dis-moi si tu veux ajouter, retirer ou reformuler des articles avant que je les charge et les vectorise à l'étape 8.6.
