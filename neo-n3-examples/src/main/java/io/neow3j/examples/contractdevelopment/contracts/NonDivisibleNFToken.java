package io.neow3j.examples.contractdevelopment.contracts;

import static io.neow3j.devpack.Helper.toByteString;
import static io.neow3j.devpack.Helper.toInt;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.StringLiteralHelper.addressToScriptHash;
import io.neow3j.devpack.CallFlags;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.ExecutionEngine;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Helper;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.events.Event4Args;

@ManifestExtra(key = "author", value = "AxLabs")
public class NonDivisibleNFToken {

    static final Hash160 contractOwner = addressToScriptHash("NUrPrFLETzoe7N2FLi2dqTvLwc9L2Em84K");
    static final String totalSupplyKey = "totalSupply";

    static final StorageContext ctx = Storage.getStorageContext();
    // Storage space to keep track of the total token supply.
    static final StorageMap totalSupplyMap = ctx.createMap("contract");
    // Maps tokens IDs to owner hashes.
    static final StorageMap tokens = ctx.createMap("tokens");
    static final StorageMap balanceMap = ctx.createMap("balance");
    static final StorageMap propertiesMap = ctx.createMap("props");

    @DisplayName("Transfer")
    static Event4Args<Hash160, Hash160, Integer, byte[]> onTransfer;

    /**
     * Gets the address of the contract owner.
     *
     * @return the address of the contract owner.
     */
    public static Hash160 contractOwner() {
        return contractOwner;
    }

    /**
     * Gets the symbol of the token.
     * 
     * @return the symbol.
     */
    public static String symbol() {
        return "ALT";
    }

    /**
     * Gets the total token supply of the contract.
     *
     * @return the total token supply.
     */
    public static int totalSupply() {
        return totalSupplyGet();
    }

    private static int totalSupplyGet() {
        return toInt(totalSupplyMap.get(totalSupplyKey));
    }

    /**
     * Gets the decimals of the token.
     * 
     * @return the decimals.
     */
    public static int decimals() {
        return 0;
    }

    /**
     * Transfers a token.
     *
     * @param to the new owner of the token.
     * @param tokenid the id of the token.
     * @return whether the transfer was successful.
     */
    public static boolean transfer(Hash160 to, byte[] tokenId) throws Exception {
        if (!to.isValid()) {
            throw new Exception("Recipient hash is invalid, i.e. not 20 bytes long.");
        }
        byte[] tokenOwnerBytes = tokens.get(tokenId);
        if (tokenOwnerBytes == null) {
            throw new Exception("The tooken with ID " + Helper.toByteString(tokenId) + 
                " does not exist.");
        }
        Hash160 tokenOwner = new Hash160(tokenOwnerBytes);
        // Only the token owner may transfer the token
        if (ExecutionEngine.getCallingScriptHash() != tokenOwner 
                && !Runtime.checkWitness(tokenOwner)) {
            return false;
        }
        // Perform the actual transfer
        tokens.put(tokenId, to.toByteArray());
        decrementBalance(tokenOwner);
        incrementBalance(to);

        // Fire event and call onNEP11Payment if the receiver is a contract.
        onTransfer.fire(tokenOwner, to, 1, tokenId);
        if (ContractManagement.getContract(to) != null) {
            Contract.call(to, "onNEP11Payment", CallFlags.ALL, new Object[]{tokenOwner, 1, tokenId});
        }
        return true;
    }

    /**
     * Gets the owner of the token with id {@code tokenid}.
     *
     * @param tokenid the id of the token.
     * @return the owner of the token.
     */
    public static Hash160 ownerOf(byte[] tokenid) {
        return getTokenOwner(tokenid);
    }

    private static Hash160 getTokenOwner(byte[] tokenid) {
        byte[] owner = tokens.get(tokenid);
        if (owner == null) {
            return null;
        }
        return new Hash160(owner);
    }

    /**
     * Creates a new token and assigns it to an account.
     * 
     * @param owner      the owner of the new token.
     * @param tokenid    the tokenid.
     * @return whether the token could be created.
     */
    public static boolean mintToken(Hash160 owner, byte[] tokenid) {
        if (owner.isValid() && checkWitness(contractOwner) && getTokenOwner(tokenid) == null) {
            tokens.put(tokenid, owner.toByteArray());
            totalSupplyMap.put(totalSupplyKey, totalSupplyGet() + 1);
            incrementBalance(owner);
            return true;
        }
        return false;
    }

    /**
     * Gets the number of tokens owned.
     *
     * @param owner the owner of the tokens.
     * @return the balance.
     */
    public static int balanceOf(Hash160 owner) {
        return getBalanceOf(owner);
    }

    public static String properties(byte[] tokenid) {
        return toByteString(propertiesMap.get(tokenid));
    }

    private static void incrementBalance(Hash160 owner) {
        balanceMap.put(owner.toByteArray(), getBalanceOf(owner) + 1);
    }

    private static void decrementBalance(Hash160 owner) {
        balanceMap.put(owner.toByteArray(), getBalanceOf(owner) - 1);
    }

    private static int getBalanceOf(Hash160 owner) {
        if (balanceMap.get(owner.toByteArray()) == null) {
            return 0;
        }
        return toInt(balanceMap.get(owner.toByteArray()));
    }

    /**
     * Destroys the contract.
     * <p>
     * Restricted to the contract owner.
     *
     * @return whether the contract was destroyed.
     */
    public static boolean destroy() {
        if (!Runtime.checkWitness(contractOwner)) {
            return false;
        }
        ContractManagement.destroy();
        return true;
    }

}