def setup():
    global k
    #fullScreen(P2D)
    size(800,800)
    fill(0,200,0,80)
    stroke(0,255,0,127)
    strokeWeight(4)
    k = 0

def draw():
    global k
    background(0)
    k += .06
    arc(width/2, height/2, 200, 200, HALF_PI, k+HALF_PI)
    if k > TWO_PI:
        exit()