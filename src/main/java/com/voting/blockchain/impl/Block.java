package com.voting.blockchain.impl;

import com.voting.blockchain.impl.utils.BlockchainUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Block {
    private static final Logger LOGGER = LoggerFactory.getLogger(Block.class);

    private int blockIndex;
    private String hash;
    private String previousHash;
    private int nonce;
    private long timestamp;
    private String merkleRoot;
    private List<Transaction> transactionsList;

    public Block() {

    }

    public Block(int blockIndex, String previousHash, long timestamp, List<Transaction> transactionsList) {
        this.blockIndex = blockIndex;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.transactionsList = transactionsList;

        merkleRoot = (transactionsList != null && !transactionsList.isEmpty())
                ? BlockchainUtils.getMerkleRoot(transactionsList)
                : BlockchainUtils.GENESIS_HASH;

        hash = BlockchainUtils.calculateHash(this);

        if (!BlockchainUtils.GENESIS_HASH.equals(previousHash)) {
            mineBlock(4);
        }
    }

    private void mineBlock(int difficulty) {
        while (!hash.substring(0, difficulty).equals(StringUtils.repeat("0", difficulty))) {
            nonce++;
            hash = BlockchainUtils.calculateHash(this);
        }
    }

    public List<Transaction> getTransactionsList() {
        return transactionsList;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public int getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "block_index: " + blockIndex + "\n"
                + "timestamp: " + timestamp + "\n"
                + "nonce: " + nonce + "\n"
                + "prev_hash: " + previousHash + "\n"
                + "hash: " + hash + "\n"
                + "merkle_root: " + merkleRoot
                + "transactions: " + transactionsList;
    }
}
