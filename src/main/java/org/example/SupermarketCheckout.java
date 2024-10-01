package org.example;

import java.util.*;

public class SupermarketCheckout {

    private final Map<Long, CheckoutStatus> customers;
    private final TreeMap<Long, LinkedList<CheckoutStatus>> lines;

    public SupermarketCheckout() {
        customers = new HashMap<>();
        lines = new TreeMap<>();
    }

    public void onCustomerEnter(long customerId, long lineNumber, long
            numItems) {
        CustomerEntity customer = new CustomerEntity(customerId, lineNumber);
        CheckoutStatus customerAction = new CheckoutStatus(customer,
                0, numItems);
        customers.put(customerId, customerAction);
        lines.computeIfAbsent(lineNumber, k -> new
                LinkedList<>()).addLast(customerAction);
    }

    public void onBasketChange(long customerId, long newNumPlaced) {
        if (newNumPlaced < 0) {
            throw new IllegalArgumentException("Number of items placed cannot be negative");
        }
        CheckoutStatus customer = customers.get(customerId);
        if (customer == null) {
            return;
        }
        if (noMoreItems(customer)) {
            onCustomerExit(customerId, customer);
            return;
        }
        long previousNumPlaced = refreshNumPlaced(newNumPlaced, customer);
        if (newItemsAdded(newNumPlaced, previousNumPlaced)) {
            moveCustomerToTheEndOfLine(customer);
        }

    }

    /**
     * Refresh the number of items placed by a customer and return the
     previous number.
     */
    private static long refreshNumPlaced(long newNumPlaced,
                                         CheckoutStatus customer) {
        long previousNumPlaced = customer.itemPlaced;
        customer.itemPlaced = newNumPlaced;
        return previousNumPlaced;
    }

    private static boolean noMoreItems(CheckoutStatus customer) {
        long remainItems = remainItems(customer);
        return remainItems <= 0;
    }

    private static long remainItems(CheckoutStatus customer) {
        return customer.itemPlaced - customer.itemProcessed;
    }

    private void moveCustomerToTheEndOfLine(CheckoutStatus customer) {
        removeCustomerFromLine(customer);
        lines.get(customer.customer.lineNumber).addLast(customer);
    }

    private static boolean newItemsAdded(long newNumPlaced, long
            previousNumPlaced) {
        return previousNumPlaced < newNumPlaced;
    }

    private void onCustomerExit(long customerId, CheckoutStatus customer) {
        removeCustomerFromLine(customer);
        customers.remove(customerId);
        notifyCustomerExited(customerId);
    }

    private void onCustomerCheckedOut(long customerId) {
        customers.remove(customerId);
        notifyCustomerCheckout(customerId);
    }

    private static void notifyCustomerExited(long customerId) {
        System.out.println("Customer " + customerId + " has exited.");
    }

    private static void notifyCustomerCheckout(long customerId) {
        System.out.println("Customer " + customerId + " has checked out.");
    }

    /**
     * @param lineNumber        the line number to service
     * @param availableCapacity the remaining items to be processed for the line
     */
    public void onLineService(long lineNumber, long availableCapacity) {
        if (availableCapacity < 0) {
            throw new IllegalArgumentException("Available capacity cannot be negative");
        }
        LinkedList<CheckoutStatus> line = lines.get(lineNumber);
        if (line == null || line.isEmpty()) {
            return;
        }
        processInternal(availableCapacity, line);
    }

    private void processInternal(long availableCapacity,
                                 LinkedList<CheckoutStatus> line) {
        while (!line.isEmpty()) {
            CheckoutStatus currentCustomer = line.peekFirst();
            long remainItems = remainItems(currentCustomer);
            if (hasEnoughCapacity(availableCapacity, remainItems)) {
                availableCapacity -= remainItems;
                checkoutCustomer(line);
            } else {
                currentCustomer.itemProcessed += availableCapacity;
                break;
            }
        }
    }

    private static boolean hasEnoughCapacity(long availableCapacity,
                                             long remainItems) {
        return availableCapacity >= remainItems;
    }

    private void checkoutCustomer(LinkedList<CheckoutStatus> line) {
        CheckoutStatus polled = line.poll();
        if (polled != null) {
            onCustomerCheckedOut(polled.customer.customerId);
        }
    }


    public void onLinesService() {
        for (Map.Entry<Long, LinkedList<CheckoutStatus>> entry :
                lines.entrySet()) {
            LinkedList<CheckoutStatus> line = entry.getValue();
            if (!line.isEmpty()) {
                consumerLineHeadIfPossible(line);
            }
        }
    }

    /**
     * A more efficient version of {@link #onLinesService()} that
     leverages Java 8 streams.
     */
    public void onLinesServiceV2() {
        lines.values().stream()
                .filter(line -> !line.isEmpty())
                .forEach(this::consumerLineHeadIfPossible);
    }

    private void consumerLineHeadIfPossible(LinkedList<CheckoutStatus> line) {
        CheckoutStatus customer = line.peekFirst();
        if (customer != null) {
            customer.itemProcessed++;
            if (noMoreItems(customer)) {
                checkoutCustomer(line);
            }
        }

    }


    private void removeCustomerFromLine(CheckoutStatus customer) {
        LinkedList<CheckoutStatus> line =
                lines.get(customer.customer.lineNumber);
        if (line != null) {
            line.remove(customer);
        }
    }

    private static class CheckoutStatus {
        public final CustomerEntity customer;
        long itemProcessed;
        long itemPlaced;

        public CheckoutStatus(CustomerEntity customer, long
                itemProcessed, long itemPlaced) {
            this.customer = customer;
            this.itemProcessed = itemProcessed;
            this.itemPlaced = itemPlaced;
        }
    }

    /**
     * A data class to wrap the immutable state of a customer checkout action.
     */
    private static class CustomerEntity {
        public final long customerId;
        public final long lineNumber;

        public CustomerEntity(long customerId, long lineNumber) {
            this.customerId = customerId;
            this.lineNumber = lineNumber;
        }
    }


    public static class Main {

        public static void main(String[] args) {
            Scanner scanner = new Scanner(System.in);

            long N = scanner.nextLong();
            scanner.nextLine();

            SupermarketCheckout checkoutTracker = new SupermarketCheckout();

            for (long i = 0; i < N; ++i) {
                String[] instruction = scanner.nextLine().split(" ");
                InstructionType type = InstructionType.valueOf(instruction[0]);
                switch (type) {
                    case CustomerEnter: {
                        long customerId = Long.parseLong(instruction[1]);
                        long lineNumber = Long.parseLong(instruction[2]);
                        long numItems = Long.parseLong(instruction[3]);
                        checkoutTracker.onCustomerEnter(customerId,
                                lineNumber, numItems);
                        break;
                    }
                    case BasketChange: {
                        long customerId = Long.parseLong(instruction[1]);
                        long newNumItems = Long.parseLong(instruction[2]);
                        checkoutTracker.onBasketChange(customerId, newNumItems);
                        break;
                    }
                    case LineService: {
                        long lineNumber = Long.parseLong(instruction[1]);
                        long numProcessedItems = Long.parseLong(instruction[2]);
                        checkoutTracker.onLineService(lineNumber,
                                numProcessedItems);
                        break;
                    }
                    case LinesService:
                        checkoutTracker.onLinesService();
                        break;
                    default:
                        System.out.println("Malformed input!");
                        System.exit(-1);
                }
            }


        }
    }

    public enum InstructionType {
        CustomerEnter,
        BasketChange,
        LineService,
        LinesService
    }
}