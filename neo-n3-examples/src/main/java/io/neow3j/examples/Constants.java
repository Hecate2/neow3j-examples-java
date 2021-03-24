package io.neow3j.examples;

import static java.util.Arrays.asList;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

public class Constants {

        // This sets up the connection to the neo-node of our private network.
        public static Neow3j NEOW3J = Neow3j.build(new HttpService("http://localhost:40332"));

        // This is Alice' account which is also available on the pre-configured private network.
        public static Account ALICE = Account.fromWIF("L24Qst64zASL2aLEKdJtRLnbnTbqpcRNWkWJ3yhDh2CLUtLdwYK2");

        // This is Bob's account which is also available on the pre-configured private network.
        public static Account BOB = Account.fromWIF("L3gSLs2CSRYss1zoTmSB9hYAxqimn7Br5yDomH8FDb6NDsupeRVK");

        // The genesis account holds all NEO and GAS at the beginning of a new Neo blockchain. The
        // genesis account is a multi-sig account. In our case it is made up of only one account,
        // namely, Alice' account.
        public static Account GENESIS = Account.createMultiSigAccount( 
            asList(ALICE.getECKeyPair().getPublicKey()), 1);

       public static Wallet WALLET = Wallet.withAccounts(ALICE, GENESIS, BOB);
    
}