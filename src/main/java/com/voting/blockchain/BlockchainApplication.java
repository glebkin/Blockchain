package com.voting.blockchain;

import com.voting.blockchain.impl.utils.BlockchainUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.voting.blockchain.impl.utils.BlockchainUtils.MASTER_NODE_URL;
import static com.voting.blockchain.impl.utils.BlockchainUtils.NGROK_API_URL;

@SpringBootApplication
public class BlockchainApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BlockchainApplication.class, args);

        try {
            String nodeUrl;

            HttpResponse httpResponse = BlockchainUtils.sendRequest(NGROK_API_URL, false, null, null);
            String jsonString = IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonString);

            JSONArray jsonArray = jsonObject.getJSONArray("tunnels");
            if (!jsonArray.isEmpty()) {
                nodeUrl = jsonArray.getJSONObject(0).get("public_url").toString();
                BlockchainUtils.nodeUrl = nodeUrl;

                JSONObject urlJson = new JSONObject();
                urlJson.put("node_url", nodeUrl);
                httpResponse = BlockchainUtils.sendRequest(MASTER_NODE_URL, true, urlJson, null);

                String responseUrls = IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                responseUrls = responseUrls.replace("\"", "")
                        .replace("[", "")
                        .replace("]", "");

                if (!responseUrls.isEmpty()) {
                    Set<String> responseUrlsList = new HashSet<>(Arrays.asList(responseUrls.split(",")));
                    responseUrlsList.remove(nodeUrl);
                    BlockchainUtils.nodesList.addAll(responseUrlsList);
                    LOGGER.info(responseUrlsList.toString());
                }
            }

        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
        }
    }
}
