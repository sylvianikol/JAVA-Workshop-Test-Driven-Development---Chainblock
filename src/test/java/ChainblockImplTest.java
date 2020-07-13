import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ChainblockImplTest {
    private Chainblock chainblock;
    private Transaction transaction;
    private List<Transaction> transactionList;

    @Before
    public void setUp() {
        this.chainblock = new ChainblockImpl();
        this.transaction = new TransactionImpl(1,
                TransactionStatus.SUCCESSFUL,
                "from", "to",
                14.78);
        this.createRandomTransactions();
    }

    private void createRandomTransactions() {
        this.transactionList = new ArrayList<>();
        TransactionStatus[] statuses = TransactionStatus.values();
        Random random = new Random(48);

        for (int i = 2; i < 22; ++i) {
            String from = Character.getName(random.nextInt(122));

            Transaction t = new TransactionImpl(i,
                    statuses[i % statuses.length],
                    "From_" + from,
                    "To_" + i,
                    new Random().nextDouble() * 99.123
            );

            this.transactionList.add(t);
        }
    }

    @Test
    public void testAddShouldIncreaseCountOfTransactions() {
        chainblock.add(transaction);

        assertEquals(1, chainblock.getCount());
    }

    @Test
    public void testAddShouldNotIncreaseCountWhenTransactionIdIsPrevAdded() {
        chainblock.add(transaction);
        chainblock.add(transaction);
        assertEquals(1, chainblock.getCount());
    }

    @Test
    public void testContainsByIdShouldReturnCorrectBooleanValue() {
        assertFalse(chainblock.contains(transaction.getId()));
        chainblock.add(transaction);
        assertTrue(chainblock.contains(transaction.getId()));
    }

    @Test
    public void testChangeTransactionStatusShouldChangeStatusOfCorrectTransaction() {
        chainblock.add(transaction);
        chainblock.changeTransactionStatus(transaction.getId(), TransactionStatus.FAILED);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChangeTransactionStatusShouldThrowIfTransactionNotPresent() {
        chainblock.changeTransactionStatus(transaction.getId(), TransactionStatus.FAILED);
    }

    @Test
    public void testRemoveByIdShouldRemoveCorrectTransaction() {
        fillChainblockWithTransactions();
        chainblock.removeTransactionById(transaction.getId());
        assertFalse(chainblock.contains(transaction.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveByIdShouldThrowIfTransactionNotPresent() {
        chainblock.add(transaction);
        chainblock.removeTransactionById(transaction.getId() + 1);
    }

    @Test
    public void testGetByIdShouldReturnCorrectTransaction() {
        fillChainblockWithTransactions();
        Transaction expected = this.transactionList.get(10);
        Transaction actual = chainblock.getById(expected.getId());
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByIdShouldThrowIfIdNotPresent() {
        fillChainblockWithTransactions();
        chainblock.getById(transactionList.get(transactionList.size() - 1).getId() + 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTransactionsByStatusShouldThrowIAEWithStatusNotPresent() {
        List<Transaction> filteredTransactions = transactionList.stream()
                .filter(t -> t.getStatus() != TransactionStatus.FAILED)
                .collect(Collectors.toList());

        assertFalse(filteredTransactions.stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.FAILED));

        for (Transaction t : filteredTransactions) {
            chainblock.add(t);
        }

        chainblock.getByTransactionStatus(TransactionStatus.FAILED);
    }

    @Test
    public void testGetTransactionsByStatusShouldReturnOnlyTransactionsWithCorrectStatus() {
        fillChainblockWithTransactions();

        List<Transaction> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESSFUL)
                .collect(Collectors.toList());

        expected.add(0, transaction);

        Iterable<Transaction> byTransactionStatus =
                chainblock.getByTransactionStatus(TransactionStatus.SUCCESSFUL);

        assertNotNull(byTransactionStatus);

        List<Transaction> actual = new ArrayList<>();
        byTransactionStatus.forEach(actual::add);

        assertEquals(expected.size(), actual.size());

        for (Transaction t : actual) {
            assertEquals(TransactionStatus.SUCCESSFUL, t.getStatus());
        }
    }

    @Test
    public void testGetTransactionsByStatusShouldReturnTransactionsInCorrectOrder() {
        fillChainblockWithTransactions();

        List<Transaction> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESSFUL)
                .collect(Collectors.toList());

        expected.add(0, transaction);
        expected.sort(Comparator.comparing(Transaction::getAmount).reversed());

        Iterable<Transaction> byTransactionStatus =
                chainblock.getByTransactionStatus(TransactionStatus.SUCCESSFUL);

        assertNotNull(byTransactionStatus);

        List<Transaction> actual = new ArrayList<>();
        byTransactionStatus.forEach(actual::add);

        assertEquals(expected.size(), actual.size());

        assertEquals(expected, actual);
    }

    @Test
    public void testGetSendersByStatusShouldReturnCorrectSenders() {
        fillChainblockWithTransactions();

        List<String> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESSFUL)
                .map(Transaction::getFrom)
                .collect(Collectors.toCollection(ArrayList::new));

        expected.add(transaction.getFrom());

        Iterable<String> senders = chainblock.getAllSendersWithTransactionStatus(TransactionStatus.SUCCESSFUL);
        assertNotNull(senders);

        List<String> actual = new ArrayList<>();
        senders.forEach(actual::add);

        assertEquals(expected.size(), actual.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSendersByTransactionStatusShouldThrowWhenNoSuchStatusPresent() {
        List<Transaction> filteredTransactions = transactionList.stream()
                .filter(t -> t.getStatus() != TransactionStatus.SUCCESSFUL)
                .collect(Collectors.toList());

        for (Transaction t : filteredTransactions) {
            chainblock.add(t);
        }

        chainblock.getAllSendersWithTransactionStatus(TransactionStatus.SUCCESSFUL);
    }

    @Test
    public void testGetSendersWithTransactionStatusShouldReturnSendersInCorrectOrder() {
        fillChainblockWithTransactions();

        List<String> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.UNAUTHORIZED)
                .sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .map(Transaction::getFrom)
                .collect(Collectors.toList());

        List<String> actual = new ArrayList<>();

        Iterable<String> senders =
                chainblock.getAllSendersWithTransactionStatus(TransactionStatus.UNAUTHORIZED);
        assertNotNull(senders);

        senders.forEach(actual::add);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetSendersWithTransactionStatusShouldReturnMultipleDuplicatedSenders() {
        fillChainblockWithTransactions();

        List<Transaction> duplicatedSenders = new ArrayList<>();

        for (int i = 0; i < 5; ++i) {
            TransactionImpl transaction = new TransactionImpl(100 + i,
                    TransactionStatus.UNAUTHORIZED,
                    "TEST_FROM",
                    "to",
                    100000000 + i);
            duplicatedSenders.add(transaction);
            chainblock.add(transaction);
        }

        transactionList.addAll(duplicatedSenders);

        List<String> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.UNAUTHORIZED)
                .sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .map(Transaction::getFrom)
                .collect(Collectors.toList());

        List<String> actual = new ArrayList<>();

        Iterable<String> senders =
                chainblock.getAllSendersWithTransactionStatus(TransactionStatus.UNAUTHORIZED);
        assertNotNull(senders);

        senders.forEach(actual::add);

        assertEquals(expected, actual);

        duplicatedSenders.sort(Comparator.comparing(Transaction::getAmount).reversed());

        for (int i = 0; i < duplicatedSenders.size(); ++i) {
            assertEquals(duplicatedSenders.get(i).getFrom(), actual.get(i));
        }
    }

    @Test
    public void testGetReceiversByStatusShouldReturnCorrectReceivers() {
        fillChainblockWithTransactions();

        List<String> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESSFUL)
                .map(Transaction::getTo)
                .collect(Collectors.toCollection(ArrayList::new));

        expected.add(transaction.getTo());

        Iterable<String> receivers = chainblock.getAllReceiversWithTransactionStatus(TransactionStatus.SUCCESSFUL);
        assertNotNull(receivers);

        List<String> actual = new ArrayList<>();
        receivers.forEach(actual::add);

        assertEquals(expected.size(), actual.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetReceiversByTransactionStatusShouldThrowWhenNoSuchStatusPresent() {
        List<Transaction> filteredTransactions = transactionList.stream()
                .filter(t -> t.getStatus() != TransactionStatus.SUCCESSFUL)
                .collect(Collectors.toList());

        for (Transaction t : filteredTransactions) {
            chainblock.add(t);
        }

        chainblock.getAllReceiversWithTransactionStatus(TransactionStatus.SUCCESSFUL);
    }

    @Test
    public void testGetReceiversWithTransactionStatusShouldReturnReceiversInCorrectOrder() {
        fillChainblockWithTransactions();

        List<String> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.UNAUTHORIZED)
                .sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .map(Transaction::getTo)
                .collect(Collectors.toList());

        List<String> actual = new ArrayList<>();

        Iterable<String> receivers =
                chainblock.getAllReceiversWithTransactionStatus(TransactionStatus.UNAUTHORIZED);
        assertNotNull(receivers);

        receivers.forEach(actual::add);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetReceiversWithTransactionStatusShouldReturnMultipleDuplicatedReceivers() {
        fillChainblockWithTransactions();

        List<Transaction> duplicatedReceivers = new ArrayList<>();

        for (int i = 0; i < 5; ++i) {
            TransactionImpl transaction = new TransactionImpl(100 + i,
                    TransactionStatus.UNAUTHORIZED,
                    "from",
                    "TEST_TO",
                    100000000 + i);
            duplicatedReceivers.add(transaction);
            chainblock.add(transaction);
        }

        transactionList.addAll(duplicatedReceivers);

        List<String> expected = transactionList.stream()
                .filter(t -> t.getStatus() == TransactionStatus.UNAUTHORIZED)
                .sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .map(Transaction::getTo)
                .collect(Collectors.toList());

        List<String> actual = new ArrayList<>();

        Iterable<String> receivers =
                chainblock.getAllReceiversWithTransactionStatus(TransactionStatus.UNAUTHORIZED);
        assertNotNull(receivers);

        receivers.forEach(actual::add);

        assertEquals(expected, actual);

        duplicatedReceivers.sort(Comparator.comparing(Transaction::getAmount).reversed());

        for (int i = 0; i < duplicatedReceivers.size(); ++i) {
            assertEquals(duplicatedReceivers.get(i).getTo(), actual.get(i));
        }
    }

    @Test
    public void testGetAllByAmountShouldReturnCorrectOrder() {
        fillChainblockWithTransactions();

        Transaction t1 = new TransactionImpl(100,
                TransactionStatus.SUCCESSFUL,
                "From",
                "To",
                100000);

        Transaction t2 = new TransactionImpl(200,
                TransactionStatus.SUCCESSFUL,
                "From",
                "To",
                100000);

        chainblock.add(t1);
        chainblock.add(t2);

        this.transactionList.add(transaction);
        this.transactionList.add(t1);
        this.transactionList.add(t2);

        List<Transaction> expected = transactionList.stream()
                .sorted(Comparator.comparing(Transaction::getAmount).reversed()
                        .thenComparing(Transaction::getId))
                .collect(Collectors.toList());

        List<Transaction> actual = new ArrayList<>();

        Iterable<Transaction> all = chainblock.getAllOrderedByAmountDescendingThenById();
        assertNotNull(all);

        all.forEach(actual::add);

        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBySenderOrderedShouldThrowWhenSenderNotPresent() {
        fillChainblockWithTransactions();
        chainblock.getBySenderOrderedByAmountDescending("Invalid_Sender");
    }

    @Test
    public void testGetBySenderOrderedShouldReturnCorrectSender() {
        fillChainblockWithTransactions();
        String sender = "Correct_Sender";

        for (int i = 0; i < 10; ++i) {
            chainblock.add(new TransactionImpl(100 + i,
                    TransactionStatus.UNAUTHORIZED,
                    sender,
                    "To",
                    1000 + 10 * i));
        }

        Iterable<Transaction> t =
                chainblock.getBySenderOrderedByAmountDescending(sender);
        assertNotNull(t);

        List<Transaction> transactions = new ArrayList<>();
        t.forEach(transactions::add);

        assertEquals(10, transactions.size());

        for (Transaction transaction : transactions) {
            assertEquals(sender, transaction.getFrom());
        }
    }

    @Test
    public void testGetBySenderOrderedShouldReturnCorrectSenderWithCorrectOrder() {
        fillChainblockWithTransactions();
        String sender = "Correct_Sender";

        for (int i = 0; i < 10; ++i) {
            chainblock.add(new TransactionImpl(100 + i,
                    TransactionStatus.UNAUTHORIZED,
                    sender,
                    "To",
                    1000 + 10 * i));
        }

        Iterable<Transaction> t =
                chainblock.getBySenderOrderedByAmountDescending(sender);
        assertNotNull(t);

        List<Transaction> transactions = new ArrayList<>();
        t.forEach(transactions::add);

        assertEquals(10, transactions.size());

        double expectedAmount = 1090;
        for (Transaction transaction : transactions) {
            assertEquals(expectedAmount, transaction.getAmount(), 0.0);
            expectedAmount -= 10;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByReceiverOrderedShouldThrowWhenReceiverNotPresent() {
        fillChainblockWithTransactions();
        chainblock.getByReceiverOrderedByAmountThenById("Invalid_Receiver");
    }

    @Test
    public void testGetByReceiverOrderedShouldReturnCorrectReceiver() {
        fillChainblockWithTransactions();
        String receiver = "Correct_Receiver";

        for (int i = 0; i < 10; ++i) {
            chainblock.add(new TransactionImpl(100 + i,
                    TransactionStatus.UNAUTHORIZED,
                    "From",
                    receiver,
                    1000 + 10 * i));
        }

        Iterable<Transaction> t =
                chainblock.getByReceiverOrderedByAmountThenById(receiver);
        assertNotNull(t);

        List<Transaction> transactions = new ArrayList<>();
        t.forEach(transactions::add);

        assertEquals(10, transactions.size());

        for (Transaction transaction : transactions) {
            assertEquals(receiver, transaction.getTo());
        }
    }

    @Test
    public void testGetByReceiverOrderedShouldReturnCorrectReceiverWithCorrectOrder() {
        fillChainblockWithTransactions();
        String receiver = "Correct_Receiver";

        List<Transaction> expected = new ArrayList<>();

        for (int i = 0; i < 10; ++i) {
            TransactionImpl transaction = new TransactionImpl(100 + i,
                    TransactionStatus.UNAUTHORIZED,
                    "From",
                    receiver,
                    1000 + 10 * i);
            expected.add(transaction);
            chainblock.add(transaction);
        }

        for (int i = 0; i < 10; ++i) {
            TransactionImpl transaction = new TransactionImpl(200 + i,
                    TransactionStatus.UNAUTHORIZED,
                    "From",
                    receiver,
                    1000 + 10 * i);
            expected.add(transaction);
            chainblock.add(transaction);
        }

        Iterable<Transaction> t =
                chainblock.getByReceiverOrderedByAmountThenById(receiver);
        assertNotNull(t);

        List<Transaction> actual = new ArrayList<>();
        t.forEach(actual::add);

        expected.sort(Comparator.comparing(Transaction::getAmount).reversed()
                .thenComparing(Transaction::getId));

        assertEquals(expected.size(), actual.size());

        assertEquals(expected, actual);
    }

    @Test
    public void testGetByTransactionStatusAndMaxAmountShouldReturnEmptyCollectionIfNoSuchTransaction() {
        fillChainblockWithTransactions();
        Iterable<Transaction> result =
                chainblock.getByTransactionStatusAndMaximumAmount(TransactionStatus.UNAUTHORIZED, -1000000);

        assertNotNull(result);

        int counter = 0;

        for (Iterator<Transaction> iter = result.iterator(); iter.hasNext(); iter.next()) {
            counter++;
        }

        assertEquals(0, counter);
    }

    @Test
    public void testGetByTransactionStatusAndMaxAmountShouldReturnCorrectTransactions() {
        fillChainblockWithTransactions();

        transactionList.add(transaction);

        List<Transaction> expected = transactionList.stream()
                .filter(t ->
                        t.getStatus() == TransactionStatus.UNAUTHORIZED
                                && t.getAmount() <= 100
                ).collect(Collectors.toList());

        Iterable<Transaction> result =
                chainblock.getByTransactionStatusAndMaximumAmount(TransactionStatus.UNAUTHORIZED, 100);
        assertNotNull(result);

        List<Transaction> actual = new ArrayList<>();

        for (Iterator<Transaction> iter = result.iterator(); iter.hasNext();) {
            actual.add(iter.next());
        }

        assertEquals(expected.size(), actual.size());

        for (Transaction t : actual) {
            assertEquals(TransactionStatus.UNAUTHORIZED, t.getStatus());
            assertTrue(Double.compare(100, t.getAmount()) >= 0);
        }
    }

    @Test
    public void testGetByTransactionStatusAndMaxAmountShouldReturnCorrectTransactionsInCorrectOrder() {
        fillChainblockWithTransactions();

        transactionList.add(transaction);

        List<Transaction> expected = transactionList.stream()
                .filter(t ->
                        t.getStatus() == TransactionStatus.UNAUTHORIZED
                                && t.getAmount() <= 100
                ).sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .collect(Collectors.toList());

        Iterable<Transaction> result =
                chainblock.getByTransactionStatusAndMaximumAmount(TransactionStatus.UNAUTHORIZED, 100);
        assertNotNull(result);

        List<Transaction> actual = new ArrayList<>();

        for (Iterator<Transaction> iter = result.iterator(); iter.hasNext();) {
            actual.add(iter.next());
        }

        assertEquals(expected, actual);

    }

//  HELPERS ///////////////////////////////////////
    private void fillChainblockWithTransactions() {
        for (Transaction t : transactionList) {
            chainblock.add(t);
        }
        chainblock.add(transaction);
    }
}