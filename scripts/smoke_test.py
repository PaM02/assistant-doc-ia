"""
Test de fumée de l'application déployée (utilisé en local et par le pipeline CI).

Vérifie le parcours complet à travers le frontend Nginx :
connexion → import d'une image scannée (OCR) → recherche sémantique → question au chatbot.

Usage : python scripts/smoke_test.py http://localhost:8090 admin <mot_de_passe> samples/procedure-panne-radio-scan.png
Option : --require-ai  échoue si le chatbot ne répond pas (sinon une IA indisponible, 503, est tolérée).
"""
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")


def call(method, url, token=None, body=None, content_type=None, timeout=180):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    if content_type:
        headers["Content-Type"] = content_type
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, json.loads(response.read() or b"null")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def multipart(path):
    boundary = uuid.uuid4().hex
    mime = "application/pdf" if path.suffix == ".pdf" else "image/png"
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{path.name}\"\r\n"
            f"Content-Type: {mime}\r\n\r\n").encode() + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    return body, f"multipart/form-data; boundary={boundary}"


def check(condition, message):
    print(("  OK    " if condition else "  ÉCHEC ") + message)
    if not condition:
        sys.exit(1)


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    require_ai = "--require-ai" in sys.argv
    base, username, password, sample = args[0].rstrip("/"), args[1], args[2], Path(args[3])

    print("1. Sécurité")
    status, _ = call("GET", f"{base}/api/documents")
    check(status == 401, f"sans jeton → 401 (reçu {status})")
    status, data = call("POST", f"{base}/api/auth/login", body=json.dumps(
        {"username": username, "password": password}).encode(), content_type="application/json")
    check(status == 200 and "token" in data, f"connexion de « {username} » (statut {status})")
    token = data["token"]

    print("2. Import et OCR")
    body, ctype = multipart(sample)
    status, doc = call("POST", f"{base}/api/documents", token, body, ctype)
    check(status == 201, f"import de {sample.name} (statut {status})")
    check(doc["extractionMethod"] == "OCR", f"méthode d'extraction = {doc['extractionMethod']}")
    check("7600" in doc["content"], "le texte reconnu contient « 7600 »")

    print("3. Recherche sémantique")
    query = urllib.parse.urlencode({"q": "Que faire si la radio tombe en panne ?", "topK": 3})
    status, hits = call("GET", f"{base}/api/search?{query}", token)
    check(status == 200 and len(hits) > 0, f"{len(hits) if isinstance(hits, list) else 0} passage(s) trouvé(s)")
    check(any(h["documentId"] == doc["id"] for h in hits), "le document importé fait partie des résultats")

    print("4. Chatbot")
    status, answer = call("POST", f"{base}/api/chat", token,
                          json.dumps({"question": "Quel code transpondeur afficher en cas de panne radio ?"}).encode(),
                          "application/json")
    if status == 200:
        check("7600" in answer["answer"], f"réponse : {answer['answer'][:100]!r}")
    elif status == 503 and not require_ai:
        print("  AVERTISSEMENT  IA indisponible (503) : clé API absente ou quota atteint — toléré")
    else:
        check(False, f"chat (statut {status}) : {answer}")

    print("5. Nettoyage")
    status, _ = call("DELETE", f"{base}/api/documents/{doc['id']}", token)
    check(status == 204, f"suppression du document de test (statut {status})")
    print("Test de fumée réussi.")


if __name__ == "__main__":
    main()
