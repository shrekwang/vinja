import random
import vim

MINE_COUNT = 10

class MineField(object):
    slots=[]
    mineList=[]
    diggedSquares=[]
    minePlayGround=[]

    def __init__(self):
        for j in range(0, 9):
            fields=[]
            for i in range(0, 9):
                self.slots.append((i,j))
                fields.append("#")
            self.minePlayGround.append(fields)
        self.mineList=random.sample(range(81),MINE_COUNT)


    def getFormatedMap(self):
        buffer=[]
        buffer.append("+-------------------+")
        for row in self.minePlayGround:
            buffer.append( "| %s |" % " ".join([str(value) for value in row]) )
        buffer.append("+-------------------+")
        return buffer

    def _digField(self,row,col):
        offsets=[ (-1,-1),(-1,0),(-1,1),(0,-1),(0,1),(1,-1),(1,0),(1,1)]
        mineCount=0
        self.diggedSquares.append(row*9+col)

        adjacentMinePos=[]
        unDiggedCount=0

        for offset in  offsets:
            i,j = row+offset[0],col+offset[1]
            pos = i * 9 + j

            if self.posValid(i,j):
                if  pos in self.mineList :
                    mineCount += 1
                    adjacentMinePos.append((i,j))
                if self.minePlayGround[i][j] == "#" :
                    unDiggedCount += 1

        if mineCount == 0:
            self.minePlayGround[row][col]=" "
        else :
            self.minePlayGround[row][col]=str(mineCount)

        if mineCount > 0 :
            if unDiggedCount ==  mineCount  :
                for pos in adjacentMinePos:
                    self.minePlayGround[pos[0]][pos[1]]="*"
        else :
            for offset in  offsets:
                i,j=row+offset[0],col+offset[1]
                if i*9+j not in self.diggedSquares:
                    if self.posValid(i,j):
                        self._digField(i,j)


    def posValid(self,i,j):
        if i < 0 or i > 8 : 
            return False
        if j < 0 or j > 8 :
            return False
        return True


    def digField(self):
        import vim
        oldX,oldY = vim.current.window.cursor
        x = oldX - 2
        y = oldY / 2 - 1

        if self.minePlayGround[x][y] != "#" :
            return

        if  x*9+y in self.mineList:
            print "this is a mine, game over."
        else :
            self._digField(x,y)

        content = self.getFormatedMap()
        output(content)
        vim.current.window.cursor=(oldX,oldY)
        self.checkAllMarked()

    def markField(self):
        import vim
        oldX,oldY = vim.current.window.cursor
        x = oldX - 2
        y = oldY / 2 - 1
        self.minePlayGround[x][y] = "*" 
        content = self.getFormatedMap()
        output(content)
        vim.current.window.cursor=(oldX,oldY)
        self.checkAllMarked()

    def checkAllMarked(self):
        import vim
        mineCount=0
        for line in vim.current.buffer:
            mineCount += line.count("*")

        if mineCount == MINE_COUNT:
            print "you made it."

class Sudoku(object):
    boardlist = []
    errorPos=None

    @staticmethod
    def runApp():
        global sudoku
        sudoku=Sudoku()
        sudoku.generate()
        content=sudoku.getMap()
        output(content)

    def generate(self, numFilled=(9*9)):
        slots = []
        fillOrder = []
        random.seed()
        # setup board
        row = [0,0,0,0,0,0,0,0,0]
        for i in range(0, 9):
            self.boardlist.append(row[:])

        for j in range(0, 9):
            for i in range(0, 9):
                slots.append((i,j))

        self.search(slots, 0)

        emptyList=random.sample(range(81),45)
        for pos in emptyList:
            x = slots[pos][0]
            y = slots[pos][1]
            self.boardlist[x][y]="."

        return self.boardlist

    def search(self, slots, index):
        nums = []
        fillOrder = []

        if len(slots) == index:
            return self.check()

        for i in range(1, 10):
            nums.append(i)

        while len(nums) > 0:
            i = random.randint(0, len(nums)-1)
            fillOrder.append(nums[i])
            del nums[i]

        for i in fillOrder:
            x = slots[index][0]
            y = slots[index][1]
            self.boardlist[x][y] = i
            if (self.check()):
                if self.search(slots, index+1):
                    return True
            self.boardlist[x][y] = 0
        return False

    def check(self):
        for i in range(0, 9):
            if (not self.checkRow(i)) or (not self.checkCol(i)) or (not self.checkSquare(i)):
                return False
        return True

    def checkCol(self, col):
        found = []
        for i in range(0, 9):
            if not self.boardlist[i][col] == 0:
                if self.boardlist[i][col] in found:
                    self.errorPos=(i+1,col+1)
                    return False
                found.append(self.boardlist[i][col])
        return True

    def checkRow(self, row):
        found = []
        for j in range(0, 9):
            if not self.boardlist[row][j] == 0:
                if self.boardlist[row][j] in found:
                    self.errorPos=(row+1,j+1)
                    return False
                found.append(self.boardlist[row][j])
        return True

    def checkSquare(self, square):
        found = []
        xoffset = (3*(square % 3))
        yoffset = int(square / 3) * 3
        for j in range(0, 3):
            for i in range(0, 3):
                if not self.boardlist[xoffset+i][yoffset+j] == 0:
                    if self.boardlist[xoffset+i][yoffset+j] in found:
                        self.errorPos=(xoffset+i+1, yoffset+j+1)
                        return False
                    found.append(self.boardlist[xoffset+i][yoffset+j])
        return True


    def getMap(self):
        map=[]
        for i in range(9):
            if  i % 3 == 0 :
                map.append( "+-------+-------+-------+" )
            group1=" ".join([str(value) for value in self.boardlist[i][0:3]])
            group2=" ".join([str(value) for value in self.boardlist[i][3:6]])
            group3=" ".join([str(value) for value in self.boardlist[i][6:9]])
            map.append( "| %s | %s | %s |" %(group1,group2,group3))
        map.append( "+-------+-------+-------+")
        return map

    def getBufferMap(self):
        buffer=vim.current.buffer
        emptyList=[0,0,0,0,0,0,0,0,0]
        board=[]

        for i in range(9):
            board.append(emptyList[:])

        yOffset=0
        for i in range(9):
            if i % 3 == 0 :
                yOffset=yOffset + 1
            row=i+yOffset

            xOffset=0
            for j in range(9):
                if j % 3 == 0 :
                    xOffset = xOffset + 2
                col=j*2+xOffset
                board[i][j]=buffer[row][col]
        return board

    def checkBufferMap(self):
        board=self.getBufferMap()
        self.boardlist=board[:]
        for i in range(9):
            for j in range(9):
                if not board[i][j].isdigit():
                    print "you haven't finished yet ! "
                    return
        if (self.check()):
            print "you made it ! "
        else :
            print "something wrong happens at %s. " %(str(self.errorPos))

    def hint(self):
        board=self.getBufferMap()

        buffer=vim.current.buffer

        for i in range(9):
            for j in range(9):
                if board[i][j] != "." :
                    continue
                num=self.getPossibleNum(i,j,board)
                if len(num) == 1:
                    hint= "%s,%s possible are [ %s ]" %( str(i+1), str(j+1), ",".join(num))
                    buffer.append(hint)

    def getPossibleNum(self, i,j,board):

        num=["1","2","3","4","5","6","7","8","9"]
        for row in range(9):
            try:
                num.remove(board[row][j])
            except :
                pass
        for col in range(9):
            try :
                num.remove(board[i][col])
            except :
                pass

        xoffset = 3 * (i / 3)
        yoffset = 3 * (j / 3)
        for row in range(0, 3):
            for col in range(0, 3):
                try :
                    num.remove(board[xoffset+row][yoffset+col])
                except:
                    pass
        return num






