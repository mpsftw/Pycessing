# This is a plain, simple file that, when loaded into the interpreter, should define a function that is runnable
# then run that function and print the return value


def simpleTestFunc(n):
    return n + 1

if __name__ == "__main__":
    print("Function returned: " + str(simpleTestFunc(2)))