"""Catálogo demonstrativo: sinopses editoriais próprias, notas ausentes até OMDb.

Identificadores permitem consultar a ficha de cada obra no IMDb. Gêneros são
categorias editoriais do CineMatch, não uma classificação oficial do IMDb.
"""
from sqlalchemy import select
from .models import CastMember, Genre, Movie

GENRES = ["Ação", "Aventura", "Comédia", "Drama", "Romance", "Terror", "Suspense", "Ficção científica", "Fantasia", "Animação", "Documentário"]
# IMDb, título brasileiro, ano, minutos, gêneros, elenco, sinopse própria.
CATALOG = [
    ("tt0133093", "Matrix", 1999, 136, ["Ação", "Ficção científica"], ["Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss"], "Um programador descobre que a realidade que conhece esconde uma simulação. Ao encontrar um grupo de rebeldes, precisa escolher entre a segurança de sua antiga vida e uma luta capaz de mudar o destino da humanidade."),
    ("tt1375666", "A Origem", 2010, 148, ["Ação", "Ficção científica", "Suspense"], ["Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page"], "Uma equipe entra em sonhos para realizar uma missão incomum: plantar uma ideia. As memórias do líder ameaçam a operação, enquanto os níveis do sonho tornam cada decisão mais arriscada."),
    ("tt0816692", "Interestelar", 2014, 169, ["Aventura", "Drama", "Ficção científica"], ["Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"], "Com a Terra cada vez menos habitável, uma expedição atravessa o espaço em busca de um novo lar. Um pai precisa conciliar a missão com a promessa de voltar para os filhos, enfrentando distâncias que também transformam o tempo."),
    ("tt0111161", "Um Sonho de Liberdade", 1994, 142, ["Drama"], ["Tim Robbins", "Morgan Freeman"], "Na prisão, dois homens constroem uma amizade e encontram maneiras de preservar a esperança diante de uma rotina brutal."),
    ("tt0109830", "Forrest Gump: O Contador de Histórias", 1994, 142, ["Drama", "Romance"], ["Tom Hanks", "Robin Wright"], "Um homem de coração generoso atravessa décadas da história americana, guiado por amizades e pelo afeto por sua companheira de infância."),
    ("tt0114709", "Toy Story", 1995, 81, ["Animação", "Aventura", "Comédia"], ["Tom Hanks", "Tim Allen"], "Os brinquedos ganham vida quando seu dono não está por perto. A chegada de um patrulheiro espacial muda o lugar de um caubói no grupo."),
    ("tt0266543", "Procurando Nemo", 2003, 100, ["Animação", "Aventura", "Comédia"], ["Albert Brooks", "Ellen DeGeneres"], "Um peixe atravessa o oceano para encontrar o filho e recebe a ajuda de uma companheira que enxerga o mundo de um jeito diferente."),
    ("tt0910970", "WALL·E", 2008, 98, ["Animação", "Ficção científica", "Romance"], ["Ben Burtt", "Elissa Knight"], "Um pequeno robô limpa uma Terra abandonada. A chegada de uma visitante do espaço desperta sua curiosidade e inicia uma jornada pela sobrevivência do planeta."),
    ("tt0245429", "A Viagem de Chihiro", 2001, 125, ["Animação", "Fantasia", "Aventura"], ["Rumi Hiiragi", "Miyu Irino"], "Uma menina entra em um mundo de espíritos e precisa trabalhar em uma casa de banhos para tentar salvar os pais e recuperar sua liberdade."),
    ("tt0120737", "O Senhor dos Anéis: A Sociedade do Anel", 2001, 178, ["Fantasia", "Aventura", "Ação"], ["Elijah Wood", "Ian McKellen", "Viggo Mortensen"], "Um hobbit recebe a tarefa de levar um anel perigoso para longe de casa. Uma sociedade se forma para acompanhá-lo em uma jornada que coloca toda a Terra-média em risco."),
    ("tt0088763", "De Volta para o Futuro", 1985, 116, ["Ficção científica", "Aventura", "Comédia"], ["Michael J. Fox", "Christopher Lloyd"], "Um adolescente viaja acidentalmente ao passado em uma máquina do tempo e interfere no encontro dos próprios pais."),
    ("tt0110912", "Pulp Fiction: Tempo de Violência", 1994, 154, ["Drama", "Suspense"], ["John Travolta", "Uma Thurman", "Samuel L. Jackson"], "Histórias de criminosos, encontros inesperados e escolhas perigosas se cruzam em Los Angeles."),
    ("tt6751668", "Parasita", 2019, 132, ["Drama", "Suspense", "Comédia"], ["Song Kang-ho", "Choi Woo-shik", "Park So-dam"], "Uma família encontra uma oportunidade de trabalhar para uma casa rica. O plano aproxima dois mundos e revela tensões escondidas sob a rotina doméstica."),
    ("tt3783958", "La La Land: Cantando Estações", 2016, 128, ["Romance", "Drama", "Comédia"], ["Ryan Gosling", "Emma Stone"], "Uma atriz e um pianista se apaixonam em Los Angeles enquanto tentam transformar seus sonhos artísticos em carreira."),
    ("tt0338013", "Brilho Eterno de uma Mente sem Lembranças", 2004, 108, ["Romance", "Drama", "Ficção científica"], ["Jim Carrey", "Kate Winslet"], "Após uma separação, um homem decide apagar as lembranças da ex-companheira. Durante o processo, redescobre o valor de memórias que queria esquecer."),
    ("tt5052448", "Corra!", 2017, 104, ["Terror", "Suspense"], ["Daniel Kaluuya", "Allison Williams"], "Uma visita à família da namorada se torna inquietante para um jovem fotógrafo. Gestos aparentemente cordiais escondem um perigo cada vez mais concreto."),
    ("tt0081505", "O Iluminado", 1980, 146, ["Terror", "Suspense", "Drama"], ["Jack Nicholson", "Shelley Duvall", "Danny Lloyd"], "Uma família passa o inverno isolada em um hotel. Enquanto o pai trabalha como zelador, o lugar parece revelar uma influência ameaçadora sobre todos."),
    ("tt1392190", "Mad Max: Estrada da Fúria", 2015, 120, ["Ação", "Aventura", "Ficção científica"], ["Tom Hardy", "Charlize Theron"], "Em um deserto governado pela escassez, uma guerreira tenta escapar de um tirano. Um viajante solitário se junta à fuga em uma perseguição pela sobrevivência."),
    ("tt0317248", "Cidade de Deus", 2002, 130, ["Drama", "Suspense"], ["Alexandre Rodrigues", "Leandro Firmino", "Phellipe Haagensen"], "Um jovem interessado em fotografia acompanha a transformação de sua comunidade no Rio de Janeiro e tenta construir um caminho próprio em meio à violência."),
    ("tt7775622", "Free Solo", 2018, 100, ["Documentário", "Aventura"], ["Alex Honnold", "Sanni McCandless"], "Um escalador se prepara para subir uma enorme parede rochosa sem cordas. O documentário acompanha o planejamento, as relações pessoais e os riscos dessa decisão."),
]


def seed(db):
    genres = {g.name: g for g in db.scalars(select(Genre)).all()}
    for name in GENRES:
        if name not in genres:
            genres[name] = Genre(name=name)
            db.add(genres[name])
    existing = set(db.scalars(select(Movie.imdb_id)).all())
    for imdb, title, year, runtime, names, cast, plot in CATALOG:
        if imdb not in existing:
            db.add(Movie(imdb_id=imdb, title=title, year=year, runtime=runtime, plot=plot,
                genres=[genres[n] for n in names], cast=[CastMember(actor_name=n, cast_order=i) for i, n in enumerate(cast)]))
    db.commit()
