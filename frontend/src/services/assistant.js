import { CLE_ACCESS } from './api'

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081/api'

/**
 * Consomme le flux SSE de POST /api/assistant/messages (etape 8.8,
 * diagramme de sequence 6).
 *
 * Ni EventSource (GET uniquement, pas d'en-tete personnalise possible, donc
 * pas de jeton Bearer) ni l'instance Axios partagee (services/api.js, pas
 * de lecture incrementale fiable d'un flux cote navigateur) ne conviennent
 * a un SSE issu d'un POST authentifie : on lit directement le corps de la
 * reponse via un ReadableStream et on reconstitue nous-memes les trames
 * "event: ...\ndata: ...\n\n" telles qu'emises par SseEmitter cote Java.
 *
 * callbacks (tous optionnels) :
 *  - surToken(texte)              : un fragment de reponse generee
 *  - surSources(tableauDeTitres)  : titres des articles cites, en fin de flux
 *  - surImpossible(message)       : reponse impossible (indisponible / hors contexte)
 */
export async function poserQuestionAssistant(
  question,
  { surToken, surSources, surImpossible, signal } = {},
) {
  const token = localStorage.getItem(CLE_ACCESS)

  const reponse = await fetch(`${BASE_URL}/assistant/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ question }),
    signal,
  })

  if (!reponse.ok || !reponse.body) {
    throw new Error(`L'assistant a repondu ${reponse.status}`)
  }

  const lecteur = reponse.body.getReader()
  const decodeur = new TextDecoder('utf-8')
  let tampon = ''

  function traiterTrame(trame) {
    let evenement = 'message'
    const lignesDonnee = []

    for (const ligne of trame.split('\n')) {
      if (ligne.startsWith('event:')) {
        evenement = ligne.slice('event:'.length).trim()
      } else if (ligne.startsWith('data:')) {
        lignesDonnee.push(ligne.slice('data:'.length).trimStart())
      }
    }

    const donnee = lignesDonnee.join('\n')
    if (!donnee) return

    switch (evenement) {
      case 'token':
        surToken?.(donnee)
        break
      case 'sources':
        try {
          surSources?.(JSON.parse(donnee))
        } catch {
          surSources?.([])
        }
        break
      case 'impossible':
        surImpossible?.(donnee)
        break
      default:
        break
    }
  }

  while (true) {
    const { done, value } = await lecteur.read()
    if (done) break
    tampon += decodeur.decode(value, { stream: true })

    let indexSeparateur
    while ((indexSeparateur = tampon.indexOf('\n\n')) !== -1) {
      const trame = tampon.slice(0, indexSeparateur)
      tampon = tampon.slice(indexSeparateur + 2)
      traiterTrame(trame)
    }
  }
}
