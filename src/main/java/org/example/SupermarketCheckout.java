package org.example;
import java.util.*;

public class SupermarketCheckout {

    private Map<Long, Customer> customers;


    private TreeMap<Long, LinkedList<Customer>> lines;

    public SupermarketCheckout(long N) {
        customers = new HashMap<>();
        lines = new TreeMap<>();
    }

    public void onCustomerEnter(long customerId, long lineNumber, long numItems) {
        Customer customer = new Customer(customerId, lineNumber, numItems,0);
        customers.put(customerId, customer);

        // Add customer to the appropriate line
        lines.computeIfAbsent(lineNumber, k -> new LinkedList<>()).addLast(customer);
    }

    public void onBasketChange(long customerId, long newNumItems) {
        Customer customer = customers.get(customerId);
        if (customer == null) return;
        customer.numItems = newNumItems;
        long remainItems = customer.numItems-customer.checkedItems;
        if (remainItems <= 0) {
            removeCustomerFromLine(customer);
            customers.remove(customerId);
            System.out.println(customerId);
        }else {
            removeCustomerFromLine(customer);
            lines.get(customer.lineNumber).addLast(customer);
        }
    }

    public void onLineService(long lineNumber, long numProcessedItems) {
        LinkedList<Customer> line = lines.get(lineNumber);
        if (line == null || line.isEmpty()) return;

        while (numProcessedItems > 0 && !line.isEmpty()) {
            Customer customer = line.peekFirst();
            long remainItems = customer.numItems-customer.checkedItems;
            if ( remainItems<= numProcessedItems) {
                numProcessedItems -= remainItems;
                line.pollFirst();
                customers.remove(customer.customerId);
                System.out.println(customer.customerId);
            } else {
                customer.checkedItems += numProcessedItems;
                numProcessedItems = 0;
            }
        }
    }

    public void onLinesService() {
        List<CustomerExitInfo> exitingCustomers = new ArrayList<>();

        for (Map.Entry<Long, LinkedList<Customer>> entry : lines.entrySet()) {
            Long lineNumber = entry.getKey();
            LinkedList<Customer> line = entry.getValue();
            if (!line.isEmpty()) {
                Customer customer = line.peekFirst();
                customer.checkedItems ++;
                long remainItems = customer.numItems-customer.checkedItems;
                if (remainItems<= 0) {
                    line.pollFirst();
                    customers.remove(customer.customerId);
                    exitingCustomers.add(new CustomerExitInfo(lineNumber, customer.customerId));
                }
            }
        }

        Collections.sort(exitingCustomers);
        for (CustomerExitInfo exitInfo : exitingCustomers) {
            System.out.println(exitInfo.customerId);
        }
    }



    private void removeCustomerFromLine(Customer customer) {
        LinkedList<Customer> line = lines.get(customer.lineNumber);
        if (line != null) {
            line.remove(customer);
        }
    }

    private static class Customer {
        long customerId;
        long lineNumber;
        long numItems;

        long checkedItems;

        public Customer(long customerId, long lineNumber, long numItems,long checkedItems) {
            this.customerId = customerId;
            this.lineNumber = lineNumber;
            this.numItems = numItems;
            this.checkedItems = checkedItems;
        }
    }

    private static class CustomerExitInfo implements Comparable<CustomerExitInfo> {
        long lineNumber;
        long customerId;

        public CustomerExitInfo(long lineNumber, long customerId) {
            this.lineNumber = lineNumber;
            this.customerId = customerId;
        }

        @Override
        public int compareTo(CustomerExitInfo other) {
            return Long.compare(this.lineNumber, other.lineNumber);
        }
    }

    public static class Main {

        public static void main(String[] args) {
            Scanner scanner = new Scanner(System.in);

            long N = scanner.nextLong();
            scanner.nextLine();

            SupermarketCheckout checkoutTracker = new SupermarketCheckout(N);

            for (long i = 0; i < N; ++i) {
                String[] instruction = scanner.nextLine().split(" ");
                String instructionType = instruction[0];

                if (instructionType.equals("CustomerEnter")) {
                    long customerId = Long.parseLong(instruction[1]);
                    long lineNumber = Long.parseLong(instruction[2]);
                    long numItems = Long.parseLong(instruction[3]);
                    checkoutTracker.onCustomerEnter(customerId, lineNumber, numItems);
                } else if (instructionType.equals("BasketChange")) {
                    long customerId = Long.parseLong(instruction[1]);
                    long newNumItems = Long.parseLong(instruction[2]);
                    checkoutTracker.onBasketChange(customerId, newNumItems);
                } else if (instructionType.equals("LineService")) {
                    long lineNumber = Long.parseLong(instruction[1]);
                    long numProcessedItems = Long.parseLong(instruction[2]);
                    checkoutTracker.onLineService(lineNumber, numProcessedItems);
                } else if (instructionType.equals("LinesService")) {
                    checkoutTracker.onLinesService();
                } else {
                    System.out.println("Malformed input!");
                    System.exit(-1);
                }
            }


        }
    }
}
