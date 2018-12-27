package com.voting.blockchain.impl;

public class Transaction {

    private int transactionIndex;
    private String sender;
    private String recipient;

    public Transaction() {

    }

    public Transaction(int transactionIndex, String sender, String recipient) {
        this.transactionIndex = transactionIndex;
        this.sender = sender;
        this.recipient = recipient;
    }

    public int getTransactionIndex() {
        return transactionIndex;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "tx_index: " + transactionIndex + "\n"
                + "sender: " + sender + "\n"
                + "recipient: " + recipient + "\n";
    }
}
