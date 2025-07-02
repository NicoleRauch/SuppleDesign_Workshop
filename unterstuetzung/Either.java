package service;

public abstract class Either<T, U> {

    public abstract boolean isError();
    public T error(){ throw new UnsupportedOperationException(); };
    public U result(){ throw new UnsupportedOperationException(); }

    public static <T, U> Either<T, U> ofError(T error) { return new Left<>(error); }
    public static <T, U> Either<T, U> ofResult(U result) { return new Right<>(result); }
}

class Right<U> extends Either {
    private final U result;
    public Right(U result) { this.result = result; }
    @Override
    public U result() { return this.result; }
    @Override
    public boolean isError() { return false; }
}

class Left<T> extends Either {
    private final T error;
    public Left(T error) { this.error = error; }
    @Override
    public T error() { return this.error; }
    @Override
    public boolean isError() { return true; }
}
