# SimpleTest.py
# Contains a bare minumum to ensure setup() and draw() run,
# function, and exit

def setup():
    size(200,200)
    background(0)
    
def draw():
    background(255)
    elipse(20,20,20,20)
    if (frameCount > 100):
        exit()