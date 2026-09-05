# Service Python - Passerelle LLM (Lot 8)

Service FastAPI isole, charge de la generation en streaming aupres de
Gemini pour l'assistant conversationnel RAG (cahier des charges, section
8.4 ; diagramme de sequence 6). N'effectue aucune recherche vectorielle ni
construction de prompt : `RagService` (Java, etapes 8.7-8.9) lui envoie un
prompt deja construit et relaie sa reponse en SSE au frontend.

## Demarrage local

    cd llm-service
    python -m venv .venv
    .venv\Scripts\activate        (source .venv/bin/activate sous Linux/macOS)
    pip install -r requirements.txt
    copy .env.example .env        (puis renseigner GEMINI_API_KEY)
    uvicorn app.main:app --reload --port 8000

## Demarrage via Docker

Le service est integre a `docker-compose.yml`, a la racine du depot,
au meme titre que les autres composants d'infrastructure :

    docker compose up -d llm-service

Les secrets sont lus depuis `llm-service/.env` (le meme fichier que pour le
demarrage local ci-dessus, `env_file` dans docker-compose.yml) : il doit
donc exister et contenir `GEMINI_API_KEY` avant de lancer la commande, sans
quoi le conteneur echoue au demarrage (`ValidationError` de `Settings`).
`docker compose ps` doit afficher `pfe-llm-service` en `healthy` une fois
`GET /health` accessible.

## Endpoints

- `GET /health` - verification basique (statut, modele configure).
- `POST /completion` - corps `{ "prompt": "...", "temperature"?: 0.3,
  "max_output_tokens"?: 1024 }`, reponse en flux SSE
  (`text/event-stream`) : une ligne `data: {"token": "..."}` par fragment
  de texte recu de Gemini, terminee par `data: [DONE]`. Un echec Gemini en
  cours de generation est renvoye comme `data: {"error": "..."}` plutot que
  comme code HTTP d'erreur, le flux etant deja ouvert a ce moment-la.

## A venir

- 8.3 : degradation controlee cote Java si ce service est injoignable.
