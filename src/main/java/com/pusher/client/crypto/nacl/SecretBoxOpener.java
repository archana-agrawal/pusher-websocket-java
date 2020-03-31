/*
Copyright 2020 Pusher Ltd
Copyright 2015 Eve Freeman

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
*/

package com.pusher.client.crypto.nacl;

import static com.pusher.client.util.internal.Preconditions.checkArgument;
import static com.pusher.client.util.internal.Preconditions.checkNotNull;

import java.util.Arrays;

public class SecretBoxOpener {

    private static final int OVERHEAD = Poly1305.TAG_SIZE;

    private byte[] key;

    public SecretBoxOpener(byte[] key) {
        checkNotNull(key, "null key passed");
        checkArgument(key.length == 32, "key length must be 32 bytes, but is " +
                key.length + " bytes");

        this.key = key;
    }

    public byte[] open(byte[] box, byte[] nonce) throws AuthenticityException {
        checkNotNull(key, "key has been cleared, create new instance");

        byte[] subKey = new byte[32];
        byte[] counter = new byte[16];
        setup(subKey, counter, nonce, key);

        // The Poly1305 key is generated by encrypting 32 bytes of zeros. Since
        // Salsa20 works with 64-byte blocks, we also generate 32 bytes of
        // keystream as a side effect.
        byte[] firstBlock = new byte[64];
        for (int i = 0; i < firstBlock.length; i++) {
            firstBlock[i] = 0;
        }
        firstBlock = Salsa.XORKeyStream(firstBlock, counter, subKey);

        byte[] poly1305Key = new byte[32];
        System.arraycopy(firstBlock, 0, poly1305Key, 0, poly1305Key.length);
        byte[] tag = new byte[Poly1305.TAG_SIZE];
        System.arraycopy(box, 0, tag, 0, tag.length);

        byte[] cipher = new byte[box.length - Poly1305.TAG_SIZE];
        System.arraycopy(box, 0 + Poly1305.TAG_SIZE, cipher, 0, cipher.length);
        if (!Poly1305.verify(tag, cipher, poly1305Key)) {
            throw new AuthenticityException();
        }

        byte[] ret = new byte[box.length - OVERHEAD];
        System.arraycopy(box, 0 + OVERHEAD, ret, 0, ret.length);
        // We XOR up to 32 bytes of box with the keystream generated from
        // the first block.
        byte[] firstMessageBlock = new byte[ret.length];
        if (ret.length > 32) {
            firstMessageBlock = new byte[32];
        }
        System.arraycopy(ret, 0, firstMessageBlock, 0, firstMessageBlock.length);
        for (int i = 0; i < firstMessageBlock.length; i++) {
            ret[i] = (byte) (firstBlock[32 + i] ^ firstMessageBlock[i]);
        }

        counter[8] = 1;
        byte[] newbox = new byte[box.length - (firstMessageBlock.length + OVERHEAD)];
        for (int i = 0; i < newbox.length; i++) {
            newbox[i] = box[i + firstMessageBlock.length + OVERHEAD];
        }
        byte[] rest = Salsa.XORKeyStream(newbox, counter, subKey);
        // Now decrypt the rest.

        System.arraycopy(rest, 0, ret, firstMessageBlock.length,
                ret.length - firstMessageBlock.length);

        return ret;
    }

    public void clearKey() {
        Arrays.fill(key, (byte) 0);
        if (key[0] != 0) {
            throw new SecurityException("key not cleared correctly");
        }
        key = null;
        // TODO: ensure implemented securely (so that the clearing code
        //  is not removed by compiler's optimisations)
    }

    // subKey = byte[32], counter = byte[16], nonce = byte[24], key = byte[32]
    private void setup(byte[] subKey, byte[] counter, byte[] nonce, byte[] key) {
        // We use XSalsa20 for encryption so first we need to generate a
        // key and nonce with HSalsa20.
        byte[] hNonce = new byte[16];
        System.arraycopy(nonce, 0, hNonce, 0, hNonce.length);
        byte[] newSubKey = Salsa.HSalsa20(hNonce, key, Salsa.SIGMA);
        System.arraycopy(newSubKey, 0, subKey, 0, subKey.length);

        System.arraycopy(nonce, 16, counter, 0, nonce.length - 16);
    }
}
