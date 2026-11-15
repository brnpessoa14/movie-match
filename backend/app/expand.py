"""Expande o cache local com dados reais da OMDb: python -m app.expand --target 500."""
import argparse
from sqlalchemy import select, func
from .database import Base, engine, SessionLocal
from .catalog import seed
from .models import Movie, ProviderUsage
from .omdb_catalog import CatalogUnavailable, fetch_many, import_movie, search_catalog

TITLES = [
    'The Godfather','The Dark Knight','Schindler\'s List','The Green Mile','The Truman Show',
    'The Lion King','Finding Dory','Inside Out','Coco','Ratatouille','Up','Soul','Moana','Frozen',
    'Zootopia','How to Train Your Dragon','Shrek','Kung Fu Panda','Spider-Man: Into the Spider-Verse',
    'Spirited Away','Princess Mononoke','My Neighbor Totoro','Your Name','Akira','Perfect Blue',
    'Dune','Dune: Part Two','Blade Runner','Blade Runner 2049','Arrival','Ex Machina','Alien','Aliens',
    '2001: A Space Odyssey','The Martian','Gravity','Star Wars','The Empire Strikes Back',
    'Avengers: Endgame','Iron Man','Guardians of the Galaxy','Logan','The Batman','Joker',
    'Whiplash','The Pianist','Amelie','Before Sunrise','Before Sunset','Pride & Prejudice','Titanic',
    'The Grand Budapest Hotel','Knives Out','Get Out','Hereditary','The Conjuring','The Exorcist',
    'A Quiet Place','The Thing','The Silence of the Lambs','Se7en','Zodiac','Gone Girl',
    'The Social Network','Good Will Hunting','Dead Poets Society','The Intouchables','Life Is Beautiful',
    'Cinema Paradiso','The Hunt','Another Round','The Lives of Others','Pan\'s Labyrinth',
    'City of God','Central Station','Elite Squad','The Second Mother','Bacurau','A Dog\'s Will',
    'Senna','Blackfish','My Octopus Teacher','The Last Dance','Man on Wire','Won\'t You Be My Neighbor?',
]
TERMS = ['love','star','night','world','man','war','time','life','king','moon','house','last','dream',
         'city','alien','heart','road','lost','dragon','magic','soul','girl','boy','dark','happy','ghost',
         'ocean','forest','summer','winter','family','music','space','journey','secret','story','dead',
         'monster','wonder','planet','dance','hotel','river','game','sky','school','wild','beautiful']

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--target',type=int,default=500)
    parser.add_argument('--requests',type=int,default=650)
    args=parser.parse_args()
    Base.metadata.create_all(engine)
    with SessionLocal() as db:
        seed(db)
        def count(): return db.scalar(select(func.count()).select_from(Movie))
        def used(): return db.scalar(select(func.coalesce(func.sum(ProviderUsage.requests),0)))
        before=count()
        starting=used()
        try:
            for offset in range(0,len(TITLES),8):
                if count()>=args.target or used()-starting+8>args.requests: break
                data=fetch_many(db,[{'t':title,'type':'movie','plot':'full'} for title in TITLES[offset:offset+8]])
                for item in data: import_movie(db,item)
                db.commit()
                print(f'Catálogo: {count()} filmes; consultas nesta execução: {used()-starting}',flush=True)
            for page in range(1,4):
                for term in TERMS:
                    if count()>=args.target or used()-starting+11>args.requests: break
                    search_catalog(db,term,page)
                    print(f'Catálogo: {count()} filmes; consultas nesta execução: {used()-starting}',flush=True)
                if count()>=args.target or used()-starting+11>args.requests: break
        except CatalogUnavailable as error:
            print(str(error),flush=True)
        print(f'Concluído: {before} para {count()} filmes; {used()-starting} consultas OMDb.',flush=True)

if __name__=='__main__': main()
