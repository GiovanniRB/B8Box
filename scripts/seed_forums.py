# seed_forums.py — povoa as comunidades e os fóruns com as contas de demonstração.
#
# Precisa das contas criadas pelo seed_demo.py (scripts/demo-accounts.local.json).
# Tudo passa pela API do site, com a mesma validação do sistema.
#
# Uso (com o app rodando):
#   python scripts/seed_forums.py                    -> cria tudo em http://localhost:8080
#   python scripts/seed_forums.py --url http://localhost:8081
#   python scripts/seed_forums.py --remover          -> apaga as comunidades daqui
#
# Pode rodar de novo: comunidade que já existe é pulada.
import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

ACCOUNTS_FILE = Path(__file__).with_name("demo-accounts.local.json")


# Cada resposta é (autor, texto, [respostas a ela])
def r(author, text, *children):
    return (author, text, list(children))


COMMUNITIES = [
    {
        "name": "MPB de Sempre",
        "category": "GENRE",
        "description": "Clássicos, lados B e discos novos da música popular brasileira.",
        "creator": "lia_vinil",
        "members": ["bea.jazz", "jorge_classico", "rafa_rimas"],
        "threads": [
            {
                "author": "lia_vinil",
                "title": "Comece por aqui: qual disco abriu a MPB para você?",
                "content": "Cada um chega na MPB por uma porta diferente. A minha foi o Clube da Esquina, "
                           "num vinil riscado do meu pai.\n\nQual foi a sua?",
                "pinned": True,
                "replies": [
                    r("jorge_classico", "Transa, do Caetano. Ouvi inteiro numa viagem e nunca mais larguei.",
                      r("lia_vinil", "Transa é outro nível. A mistura de português e inglês funciona demais.",
                        r("bea.jazz", "E os músicos! A banda toca como se estivesse na sala com você.",
                          r("jorge_classico", "Exatamente. Parece gravado ao vivo, sem pressa nenhuma.")))),
                    r("rafa_rimas", "Cheguei pelos samples. Ouvia rap e fui atrás de onde vinham aqueles trechos.",
                      r("lia_vinil", "Melhor caminho que existe. Quais discos você descobriu assim?")),
                ],
            },
            {
                "author": "bea.jazz",
                "title": "Tropicália ou Panis et Circencis ainda soa moderno?",
                "content": "Ouvi de novo esta semana e fiquei com a impressão de que os arranjos continuam "
                           "ousados, mesmo depois de tantas décadas. Exagero meu?",
                "replies": [
                    r("jorge_classico", "Nada de exagero. Os arranjos do Rogério Duprat ainda surpreendem.",
                      r("bea.jazz", "Ele fazia cada faixa parecer um pequeno filme.")),
                ],
            },
        ],
    },
    {
        "name": "Jazz para Domingos",
        "category": "GENRE",
        "description": "Para quem ouve com calma: standards, fusion e descobertas.",
        "creator": "bea.jazz",
        "members": ["lia_vinil", "jorge_classico", "tom_synth"],
        "threads": [
            {
                "author": "bea.jazz",
                "title": "Kind of Blue é mesmo o melhor ponto de partida?",
                "content": "Todo mundo indica Kind of Blue para quem quer começar no jazz. Concordo, mas "
                           "será que não vale começar por algo com mais energia?",
                "replies": [
                    r("tom_synth", "Para mim, funcionou. É um disco que não pede conhecimento nenhum.",
                      r("bea.jazz", "Justo. Talvez o segredo seja ouvir sem tentar entender.")),
                    r("lia_vinil", "Eu indicaria A Love Supreme logo depois, para mostrar o outro extremo.",
                      r("jorge_classico", "Boa dupla. Do mais calmo ao mais intenso.")),
                ],
            },
        ],
    },
    {
        "name": "Rap Nacional",
        "category": "GENRE",
        "description": "Letra em primeiro lugar. Do clássico ao lançamento da semana.",
        "creator": "rafa_rimas",
        "members": ["nina_indie", "duda_pop", "lia_vinil"],
        "threads": [
            {
                "author": "rafa_rimas",
                "title": "Sobrevivendo no Inferno: por que ainda é referência?",
                "content": "Lançado em 1997 e segue sendo citado por quase toda geração nova do rap. "
                           "O que faz esse disco durar tanto?",
                "pinned": True,
                "replies": [
                    r("lia_vinil", "A escrita. Parece crônica, e cada faixa cria um personagem.",
                      r("rafa_rimas", "Isso. E o disco tem a força de um livro: pede para ser ouvido na ordem.",
                        r("nina_indie", "Foi o primeiro disco de rap que ouvi assim, do começo ao fim."))),
                    r("duda_pop", "Vim do pop e levei um susto com o peso das letras. Recomendo a qualquer pessoa."),
                ],
            },
        ],
    },
    {
        "name": "Anos 90",
        "category": "DECADE",
        "description": "Grunge, britpop, manguebeat e tudo o que tocava na MTV.",
        "creator": "jorge_classico",
        "members": ["caio_riff", "nina_indie", "rafa_rimas", "tom_synth", "duda_pop"],
        "threads": [
            {
                "author": "caio_riff",
                "title": "1991 foi o melhor ano do rock?",
                "content": "Nevermind, Loveless, Ten, Blood Sugar Sex Magik... Tudo no mesmo ano. "
                           "Alguém consegue citar outro ano tão forte?",
                "replies": [
                    r("jorge_classico", "1971 briga de igual para igual, mas respeito a lista.",
                      r("caio_riff", "1971 é covardia, concordo. Mas para quem cresceu nos anos 90, é 1991.",
                        r("nina_indie", "Loveless sozinho já justificaria o ano.",
                          r("tom_synth", "E até hoje tem gente tentando descobrir como aquele som foi feito.",
                            r("caio_riff", "Parte da graça é não descobrir.",
                              r("nina_indie", "Concordo. Melhor ouvir no volume alto e não pensar nisso."))))),
                      r("rafa_rimas", "E no Brasil, 1994 trouxe Da Lama ao Caos. Outro marco.")),
                ],
            },
            {
                "author": "duda_pop",
                "title": "Quais hits dos anos 90 envelheceram melhor?",
                "content": "Pop também conta! Tem refrão daquela época que continua perfeito.",
                "replies": [
                    r("tom_synth", "A produção eletrônica daquela época envelheceu muito bem.",
                      r("duda_pop", "Verdade. Dá para ouvir num set de hoje sem estranhar.")),
                ],
            },
        ],
    },
    {
        "name": "Eletrônica de Quarto",
        "category": "GENRE",
        "description": "Sintetizadores, beats e produção caseira.",
        "creator": "tom_synth",
        "members": ["duda_pop", "nina_indie"],
        "threads": [
            {
                "author": "tom_synth",
                "title": "Discovery ainda é a melhor porta de entrada?",
                "content": "Sempre indico Discovery, do Daft Punk, para quem diz que não gosta de eletrônica. "
                           "Vocês têm outra sugestão?",
                "replies": [
                    r("duda_pop", "Funciona porque é pop disfarçado. Foi assim comigo.",
                      r("tom_synth", "Disfarçado é a palavra certa. Tem refrão em tudo.")),
                ],
            },
        ],
    },
    {
        "name": "Clube do Radiohead",
        "category": "ARTIST",
        "description": "Para discutir cada fase da banda, sem brigar. Ou quase.",
        "creator": "nina_indie",
        "members": ["caio_riff", "tom_synth", "jorge_classico"],
        "threads": [
            {
                "author": "nina_indie",
                "title": "OK Computer ou Kid A?",
                "content": "A pergunta clássica. Vale explicar o motivo!",
                "pinned": True,
                "replies": [
                    r("caio_riff", "OK Computer. Tem guitarra, e ponto.",
                      r("tom_synth", "Kid A é justamente quando eles largam a guitarra, e fica melhor.",
                        r("caio_riff", "Sabia que você ia dizer isso.",
                          r("nina_indie", "E é por isso que este clube existe.")))),
                    r("jorge_classico", "Ouço os dois em sequência. Faz sentido como uma história só."),
                ],
            },
        ],
    },
]


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        data = json.dumps(body).encode("utf-8") if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=60) as res:
                raw = res.read().decode("utf-8")
                return res.status, json.loads(raw) if raw[:1] in ("{", "[") else raw
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode("utf-8", "replace")

    def login(self, username, password):
        status, data = self.call("POST", "/api/auth/login", {"username": username, "password": password})
        if status != 200:
            raise RuntimeError(f"login de {username} falhou ({status}): {data}")
        return data["token"]

    def must(self, method, path, body, token):
        status, data = self.call(method, path, body, token)
        if status >= 300:
            raise RuntimeError(f"{method} {path} falhou ({status}): {data}")
        return data


class Tokens:
    def __init__(self, api):
        if not ACCOUNTS_FILE.exists():
            sys.exit("Rode antes o seed_demo.py: não encontrei demo-accounts.local.json")
        self.api = api
        self.accounts = json.loads(ACCOUNTS_FILE.read_text(encoding="utf-8"))
        self.cache = {}

    def __getitem__(self, username):
        if username not in self.cache:
            value = self.accounts[username]
            password = value["password"] if isinstance(value, dict) else value
            self.cache[username] = self.api.login(username, password)
        return self.cache[username]


def post_replies(api, tokens, thread_id, replies, parent_id=None):
    count = 0
    for author, text, children in replies:
        reply = api.must("POST", f"/api/communities/threads/{thread_id}/replies",
                         {"text": text, "parentId": parent_id}, tokens[author])
        count += 1 + post_replies(api, tokens, thread_id, children, reply["id"])
    return count


def seed(api):
    tokens = Tokens(api)
    existing = {c["name"] for c in api.must("GET", "/api/communities", None, None)}
    for c in COMMUNITIES:
        if c["name"] in existing:
            print(f"já existe: {c['name']}")
            continue
        community = api.must("POST", "/api/communities",
                             {"name": c["name"], "description": c["description"], "category": c["category"]},
                             tokens[c["creator"]])
        for member in c["members"]:
            api.must("POST", f"/api/communities/{community['id']}/join", None, tokens[member])
        replies = 0
        for t in c["threads"]:
            thread = api.must("POST", f"/api/communities/{community['id']}/threads",
                              {"title": t["title"], "content": t["content"]}, tokens[t["author"]])
            if t.get("pinned"):
                api.must("PUT", f"/api/communities/threads/{thread['id']}/pin", {"pinned": True}, tokens[c["creator"]])
            replies += post_replies(api, tokens, thread["id"], t["replies"])
        print(f"criada: {c['name']} ({len(c['threads'])} discussões, {replies} respostas)")


def remove(api):
    tokens = Tokens(api)
    current = {c["name"]: c["id"] for c in api.must("GET", "/api/communities", None, None)}
    for c in COMMUNITIES:
        if c["name"] in current:
            api.must("DELETE", f"/api/communities/{current[c['name']]}", None, tokens[c["creator"]])
            print(f"apagada: {c['name']}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser(description="Comunidades e fóruns de demonstração do B8Box")
    ap.add_argument("--url", default="http://localhost:8080")
    ap.add_argument("--remover", action="store_true")
    args = ap.parse_args()
    api = Api(args.url)
    remove(api) if args.remover else seed(api)
