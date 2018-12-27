package com.voting.blockchain.impl.utils;

import com.voting.blockchain.impl.Block;
import com.voting.blockchain.impl.Transaction;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockchainUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainUtils.class);

    public static final int TRANSACTIONS_PER_BLOCK_COUNT = 5;
    public static final String GENESIS_HASH = "1";

    private static final int TIMEOUT = 5 * 60000;
    public static final String NGROK_API_URL = "http://127.0.0.1:4041/api/tunnels";
    public static final String MASTER_NODE_URL = "https://blockchain-nodes-resolver.appspot.com/resolve";

    public static String nodeUrl;
    public static Set<String> nodesList = new HashSet<>();

    public static <T> String calculateHash(T t) {
        if (t instanceof Block) {
            return DigestUtils.sha256Hex(((Block) t).getBlockIndex()
                    + ((Block) t).getPreviousHash()
                    + ((Block) t).getTimestamp()
                    + ((Block) t).getNonce()
                    + ((Block) t).getMerkleRoot());
        } else if (t instanceof Transaction) {
            return DigestUtils.sha256Hex(((Transaction) t).getTransactionIndex()
                    + ((Transaction) t).getSender()
                    + ((Transaction) t).getRecipient());
        }
        return null;
    }

    public static HttpResponse sendRequest(String url, boolean isPost,
                                           @Nullable JSONObject requestBodyObj, @Nullable JSONArray requestBodyArr) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setConnectionRequestTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .build();

        HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

        HttpResponse httpResponse;

        try {
            if (isPost) {
                HttpPost httpPost = new HttpPost(url);
                if (requestBodyObj != null) {
                    httpPost.setHeader("Content-type", "application/json");
                    httpPost.setEntity(new StringEntity(requestBodyObj.toString()));
                } else if (requestBodyArr != null) {
                    httpPost.setHeader("Content-type", "application/json");
                    httpPost.setEntity(new StringEntity(requestBodyArr.toString()));
                }
                httpResponse = httpClient.execute(httpPost);
            } else {
                HttpGet httpGet = new HttpGet(url);
                httpResponse = httpClient.execute(httpGet);
            }

            return httpResponse;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.error("Url: {}", url);
            return null;
        }
    }


    public static String getMerkleRoot(List<Transaction> transactionsList) {
        LOGGER.info("Transactions list: {}", transactionsList);

        List<String> transactionsHashList = new ArrayList<>();
        transactionsList.forEach(transaction -> {
            String transactionHash = calculateHash(transaction);
            if (StringUtils.isNotBlank(transactionHash)) {
                transactionsHashList.add(transactionHash);
            }
        });

        return calculateMerkleRoot(transactionsHashList);
    }

    private static String calculateMerkleRoot(List<String> transactionsHashList) {
        LOGGER.info("Transactions hash list: {}", transactionsHashList);

        if (transactionsHashList.size() == 1) {
            return transactionsHashList.get(0);
        }

        if (transactionsHashList.size() % 2 == 1) {
            transactionsHashList.add(transactionsHashList.get(transactionsHashList.size() - 1));
        }

        List<String> tmpList = new ArrayList<>();
        for (int i = 0; i < transactionsHashList.size() - 1; i += 2) {
            tmpList.add(DigestUtils.sha256Hex(transactionsHashList.get(i)
                    + transactionsHashList.get(i + 1)));
        }

        return calculateMerkleRoot(tmpList);
    }
}
