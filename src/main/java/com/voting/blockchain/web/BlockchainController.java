package com.voting.blockchain.web;

import com.voting.blockchain.impl.Block;
import com.voting.blockchain.impl.Chain;
import com.voting.blockchain.impl.Transaction;
import com.voting.blockchain.impl.utils.BlockchainUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BlockchainController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainController.class);

    private final Chain chain;

    @Autowired
    public BlockchainController(Chain chain) {
        this.chain = chain;
    }

    @RequestMapping(value = "/blocks", method = RequestMethod.GET)
    public List<Block> getBlocks() {
        return chain.synchronizeChain();
    }

    @RequestMapping(value = "/result", method = RequestMethod.GET)
    public Map<String, Integer> getResult() {
        List<Block> actualChain = chain.synchronizeChain();

        Map<String, Integer> resultMap = new HashMap<>();
        actualChain.parallelStream().forEach(block -> {
            List<Transaction> transactionsList = block.getTransactionsList();
            if (transactionsList != null && !transactionsList.isEmpty()) {
                transactionsList.forEach(transaction -> {
                    String recipient = transaction.getRecipient();
                    Integer count = resultMap.get(recipient);
                    resultMap.put(recipient, count == null ? 1 : ++count);
                });
            }
        });

        return resultMap;
    }

    @RequestMapping(value = "/chain/share", method = RequestMethod.POST)
    public List<Block> shareChain(@RequestBody List<Block> blockChain) {
        chain.validateBlockChain(blockChain);
        return chain.getBlockChain();
    }

    @RequestMapping(value = "/chain/synchronize", method = RequestMethod.POST)
    public List<Block> synchronizeChain() {
        return chain.getBlockChain();
    }

    @RequestMapping(value = "/transactions/add", method = RequestMethod.POST)
    public String addTransaction(@RequestBody String body) {
        JSONObject jsonObject = new JSONObject(body);
        String sender = (String) jsonObject.get("sender");
        String recipient = (String) jsonObject.get("recipient");

        chain.addTransaction(new Transaction(chain.getTransactionPool().size(), sender, recipient));

        return HttpStatus.OK.toString();
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String registerNode(@RequestBody String body) {
        JSONObject jsonObject = new JSONObject(body);
        String nodeUrl = (String) jsonObject.get("node_url");
        if (StringUtils.isNotBlank(nodeUrl)) {
            BlockchainUtils.nodesList.add(nodeUrl);
        }

        LOGGER.info("New node connected: {}", nodeUrl);

        LOGGER.info("Nodes list: {}", BlockchainUtils.nodesList);

        return String.valueOf(HttpStatus.OK);
    }

}
