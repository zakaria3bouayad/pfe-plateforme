"""Point d'entree FastAPI - passerelle LLM (Lot 8, etape 8.1).

Service isole (cahier des charges, section 8.4) : n'expose que la
generation en streaming pour l'assistant conversationnel RAG. Aucune
logique metier ici (pas de recherche vectorielle, pas de construction de
prompt, pas d'anonymisation) : tout cela reste cote Java, dans
`RagService` (bloc C, etapes 8.7-8.9), qui est l'unique appelant prevu de
ce service.
"""

import json
import logging

from fastapi import FastAPI
from fastapi.responses import StreamingResponse

from .config import settings
from .gemini_client import GeminiError, stream_completion
from .schemas import CompletionRequest

logging.basicConfig(level=settings.log_level)
logger = logging.getLogger("llm-service")

app = FastAPI(
    title="PFE - Passerelle LLM",
    description="Service Python isole pour la generation en streaming (assistant RAG, lot 8).",
    version="0.1.0",
)


@app.get("/health")
async def health() -> dict:
    """Verification basique de disponibilite, utilisee par docker-compose
    (etape 8.2) et par la degradation controlee cote Java (etape 8.3)."""
    return {"status": "ok", "model": settings.gemini_model}


@app.post("/completion")
async def completion(request: CompletionRequest) -> StreamingResponse:
    """Genere une reponse en streaming a partir d'un prompt deja construit.

    Le flux SSE est ouvert des le premier octet renvoye : une erreur Gemini
    survenant en cours de generation ne peut donc plus se traduire par un
    code HTTP d'erreur (les en-tetes sont deja partis). Elle est a la place
    emise comme un evenement `data: {"error": ...}` avant fermeture du flux,
    a charge du RagService de la detecter et de basculer sur son propre
    scenario de repli (etape 8.3).
    """

    async def event_stream():
        try:
            async for fragment in stream_completion(
                request.prompt,
                temperature=request.temperature,
                max_output_tokens=request.max_output_tokens,
            ):
                yield f"data: {json.dumps({'token': fragment})}\n\n"
            yield "data: [DONE]\n\n"
        except GeminiError as exc:
            logger.error("Echec de generation Gemini : %s", exc)
            yield f"data: {json.dumps({'error': str(exc)})}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
