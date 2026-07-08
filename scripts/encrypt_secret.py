import os
from base64 import b64decode, b64encode
from nacl import encoding, public

public_key = os.environ["PUBLIC_KEY"]
secret_value = os.environ["SECRET_VALUE"]

pub_key = public.PublicKey(b64decode(public_key))
sealed = public.SealedBox(pub_key)
encrypted = sealed.encrypt(secret_value.encode())
print(b64encode(encrypted).decode())