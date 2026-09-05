# Passerelle LLM (Lot 8, etape 8.2).
#
# Image de developpement volontairement simple, au meme niveau de maturite
# que le reste de l'industrialisation du projet (docker-compose, pas
# d'orchestrateur) : pas de build multi-etage, pas d'utilisateur non-root,
# ce service n'ayant ni volume ni port sensible au-dela de son propre trafic.

FROM python:3.12-slim

WORKDIR /app

# Dependances copiees et installees avant le code de l'application : le
# cache Docker de cette couche reste valide tant que requirements.txt ne
# change pas, meme si app/ change a chaque etape suivante.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
