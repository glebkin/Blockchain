package com.voting.blockchain.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.blockchain.impl.utils.BlockchainUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class Chain {
    private static final Logger LOGGER = LoggerFactory.getLogger(Chain.class);

    private List<Block> blockChain = new ArrayList<>();
    private List<Transaction> transactionPool = new ArrayList<>();

    public Chain() {
        Block block = new Block(
                1,
                BlockchainUtils.GENESIS_HASH,
                0,
                null);

        blockChain.add(block);
    }

    private void createBlock() {
        Block block;

        List<Transaction> transactionsToAddList = new ArrayList<>();
        for (int i = transactionPool.size() - 1; i > transactionPool.size() - 1 - BlockchainUtils.TRANSACTIONS_PER_BLOCK_COUNT; i--) {
            transactionsToAddList.add(transactionPool.get(i));
        }

        block = new Block(
                blockChain.size() + 1,
                blockChain.get(blockChain.size() - 1).getHash(),
                System.currentTimeMillis(),
                transactionsToAddList.stream().sorted(
                        Comparator.comparingInt(Transaction::getTransactionIndex)).collect(Collectors.toList())
        );

        blockChain.add(block);
        shareBlockChain(blockChain);
    }

    private void shareBlockChain(List<Block> blockChain) {
        JSONArray jsonBlockchain = new JSONArray(blockChain);
        LOGGER.info(jsonBlockchain.toString());

        BlockchainUtils.nodesList.parallelStream().forEach((neighbourNodeUrl) ->
                BlockchainUtils.sendRequest(neighbourNodeUrl + "/chain/share",
                        true, null, jsonBlockchain));
    }

    public List<Block> getBlockChain() {
        return blockChain;
    }

    public List<Transaction> getTransactionPool() {
        return transactionPool;
    }

    public void addTransaction(Transaction transaction) {
        List<Block> newChain = synchronizeChain();
        if (newChain != null && !newChain.isEmpty()) {
            validateBlockChain(newChain);

            blockChain.parallelStream().forEach(block -> {
                List<Transaction> transactionsList = block.getTransactionsList();
                if (transactionsList != null && !transactionsList.isEmpty()) {
                    transactionPool.removeAll(transactionsList);
                }
            });
        }

        AtomicBoolean isTransactionExists = new AtomicBoolean(false);
        blockChain.parallelStream().forEach(block -> {
            List<Transaction> transactionsList = block.getTransactionsList();
            if (transactionsList != null && !transactionsList.isEmpty()) {
                transactionsList.parallelStream().forEach(confTransaction -> {
                    String confSender = confTransaction.getSender();
                    if (confSender.equals(transaction.getSender())) {
                        isTransactionExists.set(true);
                    }
                });
            }
        });

        if (!isTransactionExists.get()
                && !transactionPool.contains(transaction)) {
            transactionPool.add(transaction);

            if (transactionPool.size() > BlockchainUtils.TRANSACTIONS_PER_BLOCK_COUNT) {
                createBlock();
            }
        }
    }

    public List<Block> synchronizeChain() {
        List<Block> newChain = new ArrayList<>();
        for (String nodeUrl : BlockchainUtils.nodesList) {
            try {
                HttpResponse httpResponse = BlockchainUtils.sendRequest(nodeUrl + "/chain/synchronize", true,
                        null, null);
                if (httpResponse != null) {
                    String responseString = IOUtils.toString(
                            httpResponse.getEntity().getContent(),
                            StandardCharsets.UTF_8);

                    newChain = new ArrayList<>();
                    ObjectMapper mapper = new ObjectMapper();
                    JSONArray jsonArray = new JSONArray(responseString);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Block block = mapper.readValue(jsonObject.toString(), Block.class);
                        newChain.add(block);
                    }

                    validateBlockChain(newChain);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return !newChain.isEmpty() ? newChain : blockChain;
    }

    public void validateBlockChain(List<Block> newChain) {
        boolean isValid = false;

        if (newChain.size() > blockChain.size()) {
            for (int i = 0; i < newChain.size() - 1; i++) {
                Block block = newChain.get(i + 1);
                Block previousBlock = newChain.get(i);

                if (block.getPreviousHash().equals(previousBlock.getHash())
                        && Objects.equals(BlockchainUtils.calculateHash(block), block.getHash())
                        && Objects.equals(BlockchainUtils.calculateHash(previousBlock), previousBlock.getHash())) {

                    if (previousBlock.getBlockIndex() == 1) {
                        isValid = true;
                    } else {
                        List<Transaction> previousBlockTransactions = previousBlock.getTransactionsList();
                        if (BlockchainUtils.getMerkleRoot(previousBlockTransactions)
                                .equals(previousBlock.getMerkleRoot())) {
                            isValid = true;
                        }
                    }
                }
            }
        }

        if (isValid) {
            blockChain = new ArrayList<>(newChain);
            LOGGER.info("Blockchain was replaced with the new one: \n{}", blockChain);
        }
    }
}
