package persistence;

public record Amount(double amount) {
    public static Amount ZERO = new Amount(0);

    public Amount add(Amount amount) {
        return new Amount(this.amount + amount.amount);
    }
    public Amount subtract(Amount amount) {
        return new Amount(this.amount - amount.amount);
    }
    public boolean isMoreThan(Amount amount) {
        return this.amount > amount.amount;
    }
    public boolean isMoreThanOrEqual(Amount amount) {
        return this.amount >= amount.amount;
    }
}
