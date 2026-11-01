# seed_demo.py — povoa o B8Box com perfis e atividade de demonstração.
#
# Tudo passa pela API do próprio site (cadastro, importação do Spotify,
# avaliações, seguir, curtir, playlists), então vale a mesma validação
# do sistema e o script não precisa das credenciais do banco.
#
# Uso (com o app rodando):
#   python scripts/seed_demo.py                      -> cria tudo em http://localhost:8080
#   python scripts/seed_demo.py --url http://localhost:8081
#   python scripts/seed_demo.py --remover            -> apaga as contas de demonstração
#
# As senhas são geradas aleatoriamente e gravadas em
# scripts/demo-accounts.local.json (fora do git, ver .gitignore).
import argparse
import json
import random
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ACCOUNTS_FILE = Path(__file__).with_name("demo-accounts.local.json")

# ------------------------------------------------------------
# PERFIS (fictícios; e-mails no domínio reservado example.com)
# ------------------------------------------------------------
PROFILES = [
    {"username": "lia_vinil", "bio": "Coleciono discos desde os 15. Se tem chiado, eu gosto.",
     "genres": ["MPB", "Jazz", "Soul"]},
    {"username": "caio_riff", "bio": "Guitarra alta e volume no máximo.",
     "genres": ["Metal", "Rock"]},
    {"username": "bea.jazz", "bio": "Café, sax e domingos lentos.",
     "genres": ["Jazz", "Soul", "MPB"]},
    {"username": "tom_synth", "bio": "Produtor de fim de semana. Vivo de sintetizador.",
     "genres": ["Eletrônica", "Pop", "Indie"]},
    {"username": "rafa_rimas", "bio": "Rap nacional e gringo. Letra em primeiro lugar.",
     "genres": ["Rap", "Soul"]},
    {"username": "nina_indie", "bio": "Playlists para dias nublados.",
     "genres": ["Indie", "Rock", "Pop"]},
    {"username": "duda_pop", "bio": "Refrão grudento é arte, sim.",
     "genres": ["Pop", "Eletrônica"]},
    {"username": "jorge_classico", "bio": "Ouço álbuns inteiros, na ordem, sem pular faixa.",
     "genres": ["Rock", "MPB", "Jazz"]},
]

# ------------------------------------------------------------
# ÁLBUNS (busca no Spotify + nome do artista para conferir o resultado)
# ------------------------------------------------------------
ALBUMS = [
    ("Rock", "Abbey Road", "The Beatles"),
    ("Rock", "Rumours", "Fleetwood Mac"),
    ("Rock", "OK Computer", "Radiohead"),
    ("Metal", "Master of Puppets", "Metallica"),
    ("Metal", "Paranoid", "Black Sabbath"),
    ("Metal", "Rust in Peace", "Megadeth"),
    ("MPB", "Clube da Esquina", "Milton Nascimento"),
    ("MPB", "Construção", "Chico Buarque"),
    ("MPB", "Acabou Chorare", "Novos Baianos"),
    ("Jazz", "Kind of Blue", "Miles Davis"),
    ("Jazz", "A Love Supreme", "John Coltrane"),
    ("Soul", "What's Going On", "Marvin Gaye"),
    ("Rap", "To Pimp a Butterfly", "Kendrick Lamar"),
    ("Rap", "Sobrevivendo no Inferno", "Racionais"),
    ("Pop", "Thriller", "Michael Jackson"),
    ("Pop", "Future Nostalgia", "Dua Lipa"),
    ("Eletrônica", "Discovery", "Daft Punk"),
    ("Eletrônica", "Random Access Memories", "Daft Punk"),
    ("Indie", "Currents", "Tame Impala"),
    ("Indie", "In Rainbows", "Radiohead"),
]

# Críticas genéricas por faixa de nota
REVIEWS = {
    "alta": [
        "Um disco que funciona do começo ao fim. Não tem faixa sobrando.",
        "Ouvi de novo depois de anos e continua impressionante.",
        "A produção é impecável e as melodias ficam na cabeça por dias.",
        "Daqueles álbuns que dá vontade de mostrar pra todo mundo.",
        "Cada audição revela um detalhe novo. Obra-prima.",
        "Clássico absoluto. Merece cada elogio que recebe.",
    ],
    "media": [
        "Bom disco, com alguns momentos brilhantes e outros nem tanto.",
        "Começa muito forte, mas perde o fôlego na segunda metade.",
        "Gostei, mas esperava um pouco mais depois de tanto hype.",
        "Tem faixas excelentes, só não me pegou como um todo.",
        "Agradável de ouvir, ótimo para deixar tocando no fundo.",
    ],
    "baixa": [
        "Não é pra mim. Respeito, mas não consegui me conectar.",
        "Achei repetitivo. Talvez eu dê outra chance no futuro.",
        "Algumas ideias interessantes, mas a execução não convenceu.",
    ],
}

PLAYLISTS = [
    ("lia_vinil", "Domingo de manhã", "Para ouvir com café e janela aberta.",
     ["Kind of Blue", "Clube da Esquina", "What's Going On"]),
    ("caio_riff", "Pra quebrar o pescoço", "Só pedrada.",
     ["Master of Puppets", "Paranoid", "Rust in Peace"]),
    ("tom_synth", "Pista de dança em casa", "Grooves para dançar na sala.",
     ["Discovery", "Random Access Memories", "Future Nostalgia"]),
    ("nina_indie", "Dias nublados", "Melancolia boa.",
     ["In Rainbows", "Currents", "OK Computer"]),
    ("jorge_classico", "Discos que todo mundo devia ouvir", "Minha lista de essenciais.",
     ["Abbey Road", "Construção", "A Love Supreme", "Thriller"]),
]


# ------------------------------------------------------------
# HTTP
# ------------------------------------------------------------
class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, ok_codes=(200, 201, 204)):
        data = json.dumps(body).encode("utf-8") if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method)
        if data is not None:
            req.add_header("Content-Type", "application/json")
        if token:
            req.add_header("Authorization", "Bearer " + token)
        try:
            with urllib.request.urlopen(req, timeout=60) as res:
                raw = res.read().decode("utf-8")
                return res.status, (json.loads(raw) if raw.startswith(("{", "[")) else raw)
        except urllib.error.HTTPError as e:
            raw = e.read().decode("utf-8", "replace")
            if e.code in ok_codes:
                return e.code, raw
            return e.code, raw

    def login(self, username, password):
        status, data = self.call("POST", "/api/auth/login", {"username": username, "password": password})
        if status != 200:
            raise RuntimeError(f"login de {username} falhou ({status}): {data}")
        return data["token"]


def score_review(rng, likes_genre):
    # quem gosta do gênero tende a dar nota maior (sempre em meias estrelas)
    if likes_genre:
        score = rng.choice([3.5, 4, 4, 4.5, 4.5, 5, 5])
    else:
        score = rng.choice([2, 2.5, 3, 3, 3.5, 4, 4.5])
    tier = "alta" if score >= 4 else "media" if score >= 3 else "baixa"
    review = rng.choice(REVIEWS[tier]) if rng.random() < 0.75 else ""
    return score, review


def save_accounts(accounts):
    # só id e senha (o token de sessão não vai para o arquivo)
    data = {n: {"id": a["id"], "password": a["password"]} for n, a in accounts.items()}
    ACCOUNTS_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")


# ------------------------------------------------------------
# CRIAR
# ------------------------------------------------------------
def seed(api):
    if ACCOUNTS_FILE.exists():
        sys.exit(f"{ACCOUNTS_FILE.name} já existe: os dados de demonstração já foram criados. "
                 "Rode com --remover antes de criar de novo.")

    rng = random.Random(42)
    accounts = {}

    # 1. contas + perfil
    for p in PROFILES:
        password = secrets.token_urlsafe(12)
        status, data = api.call("POST", "/api/auth/register", {
            "username": p["username"], "email": f"{p['username']}@example.com", "password": password})
        if status != 201:
            sys.exit(f"cadastro de {p['username']} falhou ({status}): {data}")
        accounts[p["username"]] = {"id": data["id"], "password": password}
        # grava a cada conta criada, para o --remover funcionar mesmo se algo falhar no meio
        save_accounts(accounts)

        token = api.login(p["username"], password)
        accounts[p["username"]]["token"] = token
        api.call("PUT", "/api/users/me", {"bio": p["bio"], "favoriteGenres": p["genres"]}, token)
        print(f"perfil criado: {p['username']}")

    any_token = accounts[PROFILES[0]["username"]]["token"]

    # 2. álbuns (busca + importação)
    albums = {}  # título -> {id, genre}
    for genre, title, artist in ALBUMS:
        q = urllib.parse.quote(f"{title} {artist}")
        status, results = api.call("GET", f"/api/spotify/search?q={q}", token=any_token)
        if status != 200 or not results:
            print(f"  ! não achei no Spotify: {title}")
            continue
        match = next((r for r in results if artist.lower() in r["artist"].lower()), None)
        if not match:
            print(f"  ! resultado sem o artista esperado: {title}")
            continue
        status, data = api.call("POST", f"/api/spotify/import/{match['spotifyId']}", token=any_token)
        if status != 200:
            print(f"  ! falha ao importar {title} ({status})")
            continue
        albums[title] = {"id": data["album"]["id"], "genre": genre}
        print(f"álbum pronto: {match['title']} — {match['artist']}")

    # 3. avaliações
    rating_ids = []  # (id, autor)
    for p in PROFILES:
        acc = accounts[p["username"]]
        mine = [t for t, a in albums.items() if a["genre"] in p["genres"]]
        others = [t for t in albums if t not in mine]
        chosen = rng.sample(mine, min(len(mine), rng.randint(4, 6))) + rng.sample(others, rng.randint(2, 4))
        for title in chosen:
            score, review = score_review(rng, title in mine)
            status, data = api.call("POST", f"/api/ratings/album/{albums[title]['id']}",
                                    {"score": score, "review": review}, acc["token"])
            if status in (200, 201):
                rating_ids.append((data["id"], p["username"]))
        print(f"{p['username']}: {len(chosen)} avaliações")

    # 4. seguir
    names = [p["username"] for p in PROFILES]
    for name in names:
        for other in rng.sample([n for n in names if n != name], rng.randint(2, 5)):
            api.call("POST", f"/api/follows/{accounts[other]['id']}", token=accounts[name]["token"])
    print("seguidores criados")

    # 5. curtidas (cerca de 30% das avaliações dos outros)
    likes = 0
    for name in names:
        for rid, author in rating_ids:
            if author != name and rng.random() < 0.3:
                status, _ = api.call("POST", f"/api/ratings/{rid}/like", token=accounts[name]["token"])
                likes += status in (200, 201)
    print(f"{likes} curtidas")

    # 6. playlists públicas (adiciona todas as faixas de cada álbum, como o site faz)
    for owner, title, desc, album_titles in PLAYLISTS:
        token = accounts[owner]["token"]
        status, pl = api.call("POST", "/api/playlists",
                              {"title": title, "description": desc, "isPublic": True}, token)
        if status != 201:
            print(f"  ! falha ao criar playlist {title}")
            continue
        for at in album_titles:
            if at not in albums:
                continue
            _, tracks = api.call("GET", f"/api/musics/album/{albums[at]['id']}", token=token)
            for t in sorted(tracks, key=lambda m: m.get("trackNumber") or 0):
                api.call("POST", f"/api/playlists/{pl['id']}/musics/{t['id']}", token=token)
        print(f"playlist criada: {title} ({owner})")

    save_accounts(accounts)
    print(f"\nPronto. Senhas das contas em {ACCOUNTS_FILE}")


# ------------------------------------------------------------
# REMOVER
# ------------------------------------------------------------
def remove(api):
    if not ACCOUNTS_FILE.exists():
        sys.exit(f"{ACCOUNTS_FILE.name} não existe: nada para remover.")
    accounts = json.loads(ACCOUNTS_FILE.read_text(encoding="utf-8"))
    left = {}
    for name, acc in accounts.items():
        try:
            token = api.login(name, acc["password"])
            status, data = api.call("DELETE", "/api/users/me", token=token)
            if status != 200:
                raise RuntimeError(f"({status}) {data}")
            print(f"conta removida: {name}")
        except Exception as e:
            print(f"  ! não consegui remover {name}: {e}")
            left[name] = acc
    if left:
        ACCOUNTS_FILE.write_text(json.dumps(left, indent=2), encoding="utf-8")
    else:
        ACCOUNTS_FILE.unlink()
        print("todas as contas de demonstração foram removidas")


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description="Dados de demonstração do B8Box")
    ap.add_argument("--url", default="http://localhost:8080")
    ap.add_argument("--remover", action="store_true")
    args = ap.parse_args()
    api = Api(args.url)
    remove(api) if args.remover else seed(api)
