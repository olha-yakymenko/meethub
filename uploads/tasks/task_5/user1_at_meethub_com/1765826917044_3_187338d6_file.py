import time
import math
from multiprocessing import Pool, cpu_count

# -------------------------
# Zakres
# -------------------------
l = 1_000_000
r = 2_000_000


# -------------------------
# Sprawdzenie pierwszości (jak w pierwszePlus)
# -------------------------
def pierwsza(k):
    for i in range(2, k - 1):
        if i * i > k:
            return True
        if k % i == 0:
            return False
    return True


def pierwsza1(k, mlp):
    for p in mlp:
        if k % p == 0:
            return False
        if p * p > k:
            return True
    return True


# -------------------------
# Tworzenie listy małych liczb pierwszych
# -------------------------
def male_pierwsze(r):
    mlp = []
    s = math.ceil(math.sqrt(r))
    for i in range(2, s + 1):
        if pierwsza(i):
            mlp.append(i)
    return mlp


# =====================================================
# SEKWENCYJNE LICZENIE BLIŹNIACZYCH LICZB PIERWSZYCH
# =====================================================
def blizniacze_sekwencyjnie(l, r):
    mlp = male_pierwsze(r)
    blizniacze = []

    for i in range(l, r - 1):
        if pierwsza1(i, mlp) and pierwsza1(i + 2, mlp):
            blizniacze.append((i, i + 2))

    return blizniacze


# =====================================================
# RÓWNOLEGŁE LICZENIE (funkcja dla Pool)
# =====================================================
def blizniacze_fragment(args):
    start, end, mlp = args
    wynik = []

    for i in range(start, end - 1):
        if pierwsza1(i, mlp) and pierwsza1(i + 2, mlp):
            wynik.append((i, i + 2))

    return wynik


def blizniacze_rownolegle(l, r, nproc):
    mlp = male_pierwsze(r)

    size = (r - l) // nproc
    zadania = []

    for i in range(nproc):
        start = l + i * size
        end = r if i == nproc - 1 else start + size
        zadania.append((start, end, mlp))

    # with Pool(processes=nproc) as pool:
    #     wyniki = pool.map(blizniacze_fragment, zadania)

    pool = Pool(processes=nproc)
    wyniki = pool.map(blizniacze_fragment, zadania)
    pool.close()
    pool.join()

    blizniacze = []
    for w in wyniki:
        blizniacze.extend(w)

    return blizniacze


# =====================================================
# MAIN – pomiar czasu
# =====================================================
if __name__ == "__main__":
    print("Zakres:", l, r)

    # ---- sekwencyjnie ----
    t0 = time.time()
    seq = blizniacze_sekwencyjnie(l, r)
    t1 = time.time()
    print("Sekwencyjnie:", t1 - t0, "s")

    # ---- równolegle ----
    nproc = cpu_count()
    t2 = time.time()
    par = blizniacze_rownolegle(l, r, nproc)
    t3 = time.time()
    print("Równolegle (", nproc, "procesów):", t3 - t2, "s")

    print("Liczba bliźniaczych par:", len(seq))
    print(seq)
