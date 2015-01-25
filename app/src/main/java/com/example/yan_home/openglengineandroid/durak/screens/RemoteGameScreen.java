package com.example.yan_home.openglengineandroid.durak.screens;

import com.example.yan_home.openglengineandroid.durak.communication.game_server.GameServerCommunicator;
import com.example.yan_home.openglengineandroid.durak.communication.game_server.IGameServerConnector;
import com.example.yan_home.openglengineandroid.durak.entities.cards.Card;
import com.example.yan_home.openglengineandroid.durak.entities.cards.CardsHelper;
import com.example.yan_home.openglengineandroid.durak.input.cards.CardsTouchProcessor;
import com.example.yan_home.openglengineandroid.durak.layouting.CardsLayoutSlot;
import com.example.yan_home.openglengineandroid.durak.layouting.CardsLayouter;
import com.example.yan_home.openglengineandroid.durak.layouting.impl.CardsLayouterSlotImpl;
import com.example.yan_home.openglengineandroid.durak.layouting.impl.PlayerCardsLayouter;
import com.example.yan_home.openglengineandroid.durak.layouting.threepoint.ThreePointFanLayouter;
import com.example.yan_home.openglengineandroid.durak.nodes.CardNode;
import com.example.yan_home.openglengineandroid.durak.protocol.BaseProtocolMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.data.CardData;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.CardMovedProtocolMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.GameSetupProtocolMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.PlayerTakesActionMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.RequestCardForAttackMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.RequestRetaliatePilesMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.ResponseCardForAttackMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.ResponseRetaliatePilesMessage;
import com.example.yan_home.openglengineandroid.durak.protocol.messages.RetaliationInvalidProtocolMessage;
import com.example.yan_home.openglengineandroid.durak.tweening.CardsTweenAnimator;
import com.yan.glengine.nodes.YANTexturedNode;
import com.yan.glengine.nodes.YANTexturedScissorNode;
import com.yan.glengine.renderer.YANGLRenderer;
import com.yan.glengine.util.geometry.YANReadOnlyVector2;
import com.yan.glengine.util.geometry.YANVector2;
import com.yan.glengine.util.math.YANMathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Yan-Home on 10/3/2014.
 */
public class RemoteGameScreen extends BaseGameScreen {

    private static final int CARDS_COUNT = 36;
    private static final int MAX_CARDS_IN_LINE = 8;

    //TODO : change to magic enumeration ?
    //  http://tools.android.com/tech-docs/support-annotations
    //  http://blog.jetbrains.com/idea/2012/02/new-magic-constant-inspection/

    //pile indices (it is hard coded to have 3 players)
    public static final int STOCK_PILE_INDEX = 0;
    public static final int DISCARD_PILE_INDEX = 1;
    public static final float CARD_SCALE_AMOUNT_OPPONENT = 0.6f;

    //default values may change
    public static int CURRENT_PLAYER_PILE_INDEX = -1;
    public static int PLAYER_TO_THE_RIGHT_PILE_INDEX = -1;
    public static int PLAYER_TO_THE_LEFT_PILE_INDEX = -1;

    //Player hand related
    private CardsLayouter mPlayerCardsLayouter;
    private CardsTouchProcessor mCardsTouchProcessor;

    private ThreePointFanLayouter mThreePointFanLayouterPlayerTwo;
    private ThreePointFanLayouter mThreePointFanLayouterPlayerThree;

    /**
     * Actual Texture nodes that hold necessary card data
     */
    private Map<Card, CardNode> mCardNodes;


    private CardsTweenAnimator mCardsTweenAnimator;
    private YANTexturedNode mBackOfCardNode;


    private int mTopCardOnFieldSortingLayer = HIGHEST_SORTING_LAYER;

    /**
     * Mapping between pile index and array of cards in it
     */
    private Map<Integer, ArrayList<Card>> mPileIndexToCardListMap;

    /**
     * Mapping between pile index and position of the pile
     */
    private Map<Integer, YANVector2> mPileIndexToPositionMap;

    //cached card dimensions
    private float mCardWidth;
    private float mCardHeight;

    //cached player texture nodes of cards
    private ArrayList<CardNode> mPlayerOneCardNodes;
    private ArrayList<CardNode> mPlayerTwoTextureNodeCards;
    private ArrayList<CardNode> mPlayerThreeTextureNodeCards;


    private ArrayList<YANTexturedNode> mAvatarPlaceHoldersArray;
    private ArrayList<YANTexturedNode> mCockPlaceHoldersArray;
    private boolean mCardForAttackRequested;
    private boolean mRequestedRetaliation;

    private YANTexturedScissorNode mScissorCockNode;
    private float mScissoringCockVisibleStartY;

    private IGameServerConnector mGameServerConnector;

    public RemoteGameScreen(YANGLRenderer renderer) {
        super(renderer);

        //TODO : inject game server connector
        mGameServerConnector = new GameServerCommunicator();
        mGameServerConnector.setListener(new IGameServerConnector.IGameServerCommunicatorListener() {
            @Override
            public void handleServerMessage(BaseProtocolMessage serverMessage) {

                //TODO : this is not an efficient way to handle messages
                if (serverMessage.getMessageName().equals(CardMovedProtocolMessage.MESSAGE_NAME)) {
                    handleCardMoveMessage((CardMovedProtocolMessage) serverMessage);
                } else if (serverMessage.getMessageName().equals(RequestCardForAttackMessage.MESSAGE_NAME)) {
                    handleRequestCardForAttackMessage((RequestCardForAttackMessage) serverMessage);
                } else if (serverMessage.getMessageName().equals(RequestRetaliatePilesMessage.MESSAGE_NAME)) {
                    handleRequestRetaliatePilesMessage((RequestRetaliatePilesMessage) serverMessage);
                } else if (serverMessage.getMessageName().equals(GameSetupProtocolMessage.MESSAGE_NAME)) {
                    handleGameSetupMessage((GameSetupProtocolMessage) serverMessage);
                } else if (serverMessage.getMessageName().equals(PlayerTakesActionMessage.MESSAGE_NAME)) {
                    handlePlayerTakesActionMessage((PlayerTakesActionMessage) serverMessage);
                } else if (serverMessage.getMessageName().equals(RetaliationInvalidProtocolMessage.MESSAGE_NAME)) {
                    handleInvalidRetaliationMessage((RetaliationInvalidProtocolMessage) serverMessage);
                }
            }
        });


        mCardNodes = new HashMap<>(CARDS_COUNT);

        mPileIndexToCardListMap = new HashMap<>(CARDS_COUNT / 2);
        mPileIndexToPositionMap = new HashMap<>(CARDS_COUNT / 2);
        mCardsTweenAnimator = new CardsTweenAnimator();


        //array holds avatars for each player
        mAvatarPlaceHoldersArray = new ArrayList<>();
        mCockPlaceHoldersArray = new ArrayList<>();

        //init 3 points layouter to create a fan of opponents hands
        mThreePointFanLayouterPlayerTwo = new ThreePointFanLayouter(2);
        mThreePointFanLayouterPlayerThree = new ThreePointFanLayouter(2);

        //init player cards layouter
        mPlayerCardsLayouter = new PlayerCardsLayouter(CARDS_COUNT);

        //allocate temp array of texture cards
        mPlayerOneCardNodes = new ArrayList<>(36);
        mPlayerTwoTextureNodeCards = new ArrayList<>(36);
        mPlayerThreeTextureNodeCards = new ArrayList<>(36);

        //currently we are initializing with empty array , cards will be set every time player pile content changes
        mCardsTouchProcessor = new CardsTouchProcessor(mPlayerOneCardNodes, mCardsTweenAnimator);

        //set listener to handle touches
        mCardsTouchProcessor.setCardsTouchProcessorListener(new CardsTouchProcessor.CardsTouchProcessorListener() {
            @Override
            public void onSelectedCardTap(CardNode cardNode) {

                //TODO : here we are taking a cardNode
                if (mRequestedRetaliation) {
                    mRequestedRetaliation = false;

                    ResponseRetaliatePilesMessage responseRetaliatePilesMessage = new ResponseRetaliatePilesMessage(new ArrayList<List<Card>>());
                    mGameServerConnector.sentMessageToServer(responseRetaliatePilesMessage);
                }

            }

            @Override
            public void onDraggedCardReleased(CardNode cardNode) {
                if (mCardForAttackRequested) {
                    mCardForAttackRequested = false;

                    ResponseCardForAttackMessage responseCardForAttackMessage = new ResponseCardForAttackMessage(cardNode.getCard());
                    mGameServerConnector.sentMessageToServer(responseCardForAttackMessage);

                } else if (mRequestedRetaliation) {
                    mRequestedRetaliation = false;

                    //TODO : For now we are "assuming" there is only one field pile always
                    List<List<Card>> list = new ArrayList<>();
                    Card cardOnFiled = mPileIndexToCardListMap.get(5).get(0);
                    List<Card> innerList = new ArrayList<>();
                    innerList.add(cardNode.getCard());
                    innerList.add(cardOnFiled);
                    list.add(innerList);
                    ResponseRetaliatePilesMessage responseRetaliatePilesMessage = new ResponseRetaliatePilesMessage(list);
                    mGameServerConnector.sentMessageToServer(responseRetaliatePilesMessage);
                } else {
                    layoutPlayerOneCards();
                }
            }
        });
    }


    @Override
    public void onSetActive() {
        super.onSetActive();
        mCardsTouchProcessor.register();
        mGameServerConnector.connect();
    }

    @Override
    public void onSetNotActive() {
        super.onSetNotActive();
        mCardsTouchProcessor.unRegister();
        mGameServerConnector.disconnect();
    }

    @Override
    protected void onAddNodesToScene() {
        super.onAddNodesToScene();

        //add card nodes
        for (CardNode cardNode : mCardNodes.values()) {
            addNode(cardNode);
        }

        for (YANTexturedNode avatar : mAvatarPlaceHoldersArray) {
            addNode(avatar);
        }

        for (YANTexturedNode cock : mCockPlaceHoldersArray) {
            addNode(cock);
        }

        //add Scissoring cock node
        addNode(mScissorCockNode);

    }


    @Override
    protected void onLayoutNodes() {
        super.onLayoutNodes();

        //layout avatars
        float offsetX = getSceneSize().getX() * 0.01f;

        //setup avatar for player at left top
        YANTexturedNode avatar = mAvatarPlaceHoldersArray.get(0);
        avatar.setAnchorPoint(1f, 1f);
        avatar.setSortingLayer(HIGHEST_SORTING_LAYER + 1);
        avatar.setPosition(getSceneSize().getX() - offsetX, getSceneSize().getY() - offsetX);
        mCockPlaceHoldersArray.get(0).setSortingLayer(avatar.getSortingLayer());
        mCockPlaceHoldersArray.get(0).setPosition(avatar.getPosition().getX() - avatar.getSize().getX() / 2 - mCockPlaceHoldersArray.get(0).getSize().getX() / 2,
                avatar.getPosition().getY() - avatar.getSize().getY() - mCockPlaceHoldersArray.get(0).getSize().getY());

        //setup avatar for player at right top
        float topOffset = getSceneSize().getY() * 0.07f;
        avatar = mAvatarPlaceHoldersArray.get(1);
        avatar.setAnchorPoint(1f, 0f);
        avatar.setSortingLayer(HIGHEST_SORTING_LAYER + 1);
        avatar.setPosition(getSceneSize().getX() - offsetX, topOffset);
        mCockPlaceHoldersArray.get(1).setSortingLayer(avatar.getSortingLayer());
        mCockPlaceHoldersArray.get(1).setPosition(avatar.getPosition().getX() - avatar.getSize().getX() / 2 - mCockPlaceHoldersArray.get(1).getSize().getX() / 2,
                avatar.getPosition().getY() - mCockPlaceHoldersArray.get(1).getSize().getY());

        //setup 3 points for player at right top
        float fanDistance = getSceneSize().getX() * 0.05f;

        YANVector2 origin = new YANVector2(avatar.getPosition().getX() - avatar.getSize().getX(), avatar.getPosition().getY());
        YANVector2 leftBasis = new YANVector2(origin.getX(), origin.getY() + fanDistance);
        YANVector2 rightBasis = new YANVector2(origin.getX() - fanDistance, origin.getY());
        mThreePointFanLayouterPlayerTwo.setThreePoints(origin, leftBasis, rightBasis);

        //third player avatar
        avatar = mAvatarPlaceHoldersArray.get(2);
        avatar.setAnchorPoint(0f, 0f);
        avatar.setSortingLayer(HIGHEST_SORTING_LAYER + 1);
        avatar.setPosition(offsetX, topOffset);
        mCockPlaceHoldersArray.get(2).setSortingLayer(avatar.getSortingLayer());
        mCockPlaceHoldersArray.get(2).setPosition(avatar.getPosition().getX() + mCockPlaceHoldersArray.get(2).getSize().getX() / 2, avatar.getPosition().getY() - mCockPlaceHoldersArray.get(2).getSize().getY());

        //setup 3 points for player at left top
        origin = new YANVector2(avatar.getPosition().getX() /*+ avatar.getSize().getX()*/, avatar.getPosition().getY());
        leftBasis = new YANVector2(origin.getX() + fanDistance, origin.getY());
        rightBasis = new YANVector2(origin.getX(), origin.getY() + fanDistance);
        mThreePointFanLayouterPlayerThree.setThreePoints(origin, leftBasis, rightBasis);

        //swap direction
        mThreePointFanLayouterPlayerThree.setDirection(ThreePointFanLayouter.LayoutDirection.RTL);

        //filled in cock by default is out of the screen
        mScissorCockNode.setPosition(-getSceneSize().getX(), 0);
    }

    private void layoutPile(int pileIndex, float x, float y, int rotationZ, float sizeScale) {
        ArrayList<Card> cardsInStockPile = mPileIndexToCardListMap.get(pileIndex);
        for (Card card : cardsInStockPile) {
            CardNode cardTexturedNode = mCardNodes.get(card);
            layoutCard(x, y, rotationZ, sizeScale, cardTexturedNode);
        }

        //init pile position
        mPileIndexToPositionMap.put(pileIndex, new YANVector2(x, y));
    }

    private void layoutCard(float x, float y, int rotationZ, float sizeScale, CardNode cardTexturedNode) {
        cardTexturedNode.setPosition(x, y);
        cardTexturedNode.setRotationZ(rotationZ);
        cardTexturedNode.setSize(mCardWidth * sizeScale, mCardHeight * sizeScale);
    }


    @Override
    protected void onChangeNodesSize() {
        super.onChangeNodesSize();

        //cards
        float aspectRatio = mBackOfCardNode.getTextureRegion().getWidth() / mBackOfCardNode.getTextureRegion().getHeight();
        mCardWidth = Math.min(getSceneSize().getX(), getSceneSize().getY()) / (float) ((MAX_CARDS_IN_LINE) / 2);
        mCardHeight = mCardWidth / aspectRatio;

        //set size of a card for touch processor
        mCardsTouchProcessor.setOriginalCardSize(mCardWidth, mCardHeight);

        //set size for each card
        for (YANTexturedNode cardNode : mCardNodes.values()) {
            cardNode.setSize(mCardWidth, mCardHeight);
        }

        //init the player cards layouter
        mPlayerCardsLayouter.init(mCardWidth, mCardHeight,
                //maximum available width
                getSceneSize().getX(),
                //base x position ( center )
                getSceneSize().getX() / 2,
                //base y position
                getSceneSize().getY() - mFence.getSize().getY() / 2);

        //set avatars sizes
        YANTexturedNode avatar = mAvatarPlaceHoldersArray.get(0);
        aspectRatio = avatar.getTextureRegion().getWidth() / avatar.getTextureRegion().getHeight();
        float newWidth = getSceneSize().getX() * 0.2f;
        float newHeight = newWidth / aspectRatio;
        for (YANTexturedNode node : mAvatarPlaceHoldersArray) {
            node.setSize(newWidth, newHeight);
        }

        //set cock sizes
        YANTexturedNode cock = mCockPlaceHoldersArray.get(0);
        aspectRatio = cock.getTextureRegion().getWidth() / cock.getTextureRegion().getHeight();
        newWidth = getSceneSize().getX() * 0.1f;
        newHeight = newWidth / aspectRatio;
        for (YANTexturedNode node : mCockPlaceHoldersArray) {
            node.setSize(newWidth, newHeight);
        }

        mScissorCockNode.setSize(newWidth, newHeight);

    }

    @Override
    protected void onCreateNodes() {
        super.onCreateNodes();

        initCardsMap();

        //add 3 avatars for 3 players
        for (int i = 0; i < 3; i++) {
            mAvatarPlaceHoldersArray.add(new YANTexturedNode(mUiAtlas.getTextureRegion("stump.png")));
            mAvatarPlaceHoldersArray.get(i).setSortingLayer(HIGHEST_SORTING_LAYER);
            mCockPlaceHoldersArray.add(new YANTexturedNode(mUiAtlas.getTextureRegion("grey_cock.png")));
            mCockPlaceHoldersArray.get(i).setSortingLayer(HIGHEST_SORTING_LAYER);
        }

        //top left cock is looking the other way
        mCockPlaceHoldersArray.get(2).setRotationY(180);

        //scissor cock node that will be used to present the fill in for cocks
        mScissorCockNode = new YANTexturedScissorNode(mUiAtlas.getTextureRegion("yellow_cock.png"));
        mScissorCockNode.setSortingLayer(HIGHEST_SORTING_LAYER + 1);

    }

    private void initCardsMap() {
        mBackOfCardNode = new YANTexturedNode(mCardsAtlas.getTextureRegion("cards_back.png"));
        ArrayList<Card> cardEntities = CardsHelper.create36Deck();

        for (Card card : cardEntities) {
            String name = "cards_" + card.getSuit() + "_" + card.getRank() + ".png";
            CardNode cardNode = new CardNode(mCardsAtlas.getTextureRegion(name), mBackOfCardNode.getTextureRegion(), card);
            mCardNodes.put(card, cardNode);

            //hide the card
            cardNode.useBackTextureRegion();
        }

        //put everything in the stock pile
        mPileIndexToCardListMap.put(STOCK_PILE_INDEX, cardEntities);

        //init rest of a piles
        mPileIndexToCardListMap.put(DISCARD_PILE_INDEX, new ArrayList<Card>(CARDS_COUNT));
    }

    @Override
    public void onUpdate(float deltaTimeSeconds) {
        super.onUpdate(deltaTimeSeconds);

        mGameServerConnector.update();
        mCardsTweenAnimator.update(deltaTimeSeconds * 1);

        //animate scissoring cock
        mScissorCockNode.setVisibleArea(0, mScissoringCockVisibleStartY, 1, 1);
        mScissoringCockVisibleStartY += 0.001;
        if (mScissoringCockVisibleStartY > 1.0)
            mScissoringCockVisibleStartY = 0.0f;
    }


    private void handleInvalidRetaliationMessage(RetaliationInvalidProtocolMessage retaliationInvalidProtocolMessage) {
        //TODO : as long as we have only one pile we can just relayout player one piles.Later we will have to
        //search what should get back to player hand and what is not
        layoutPlayerOneCards();
    }

    private void handlePlayerTakesActionMessage(PlayerTakesActionMessage playerTakesActionMessage) {
        int actionPlayerIndex = playerTakesActionMessage.getMessageData().getPlayerIndex();

        //since we don't have reference to players indexes in the game
        //we translating the player index to pile index
        int actionPlayerPileIndex = (actionPlayerIndex + 2) % 5;

        YANReadOnlyVector2 newCockPosition = null;
        int rotationAngle = 0;

        if (actionPlayerPileIndex == CURRENT_PLAYER_PILE_INDEX) {
            newCockPosition = mCockPlaceHoldersArray.get(0).getPosition();
            rotationAngle = 0;
        } else if (actionPlayerPileIndex == PLAYER_TO_THE_RIGHT_PILE_INDEX) {
            newCockPosition = mCockPlaceHoldersArray.get(1).getPosition();
            rotationAngle = 0;
        } else if (actionPlayerPileIndex == PLAYER_TO_THE_LEFT_PILE_INDEX) {
            newCockPosition = mCockPlaceHoldersArray.get(2).getPosition();
            rotationAngle = 180;
        }

        mScissorCockNode.setPosition(newCockPosition.getX(), newCockPosition.getY());
        mScissorCockNode.setRotationY(rotationAngle);

        //start animating the cock down
        mScissoringCockVisibleStartY = 1;
    }

    private void handleGameSetupMessage(GameSetupProtocolMessage gameSetupProtocolMessage) {

        //TODO : get rid of the statics and make it more generic
        CURRENT_PLAYER_PILE_INDEX = gameSetupProtocolMessage.getMessageData().getMyPileIndex();
        PLAYER_TO_THE_RIGHT_PILE_INDEX = (CURRENT_PLAYER_PILE_INDEX + 1) % 5;
        PLAYER_TO_THE_LEFT_PILE_INDEX = (CURRENT_PLAYER_PILE_INDEX + 2) % 5;

        //TODO : encapsulate
        mPileIndexToCardListMap.put(CURRENT_PLAYER_PILE_INDEX, new ArrayList<Card>(CARDS_COUNT));
        mPileIndexToCardListMap.put(PLAYER_TO_THE_RIGHT_PILE_INDEX, new ArrayList<Card>(CARDS_COUNT));
        mPileIndexToCardListMap.put(PLAYER_TO_THE_LEFT_PILE_INDEX, new ArrayList<Card>(CARDS_COUNT));

        //extract trump card
        CardData cardData = gameSetupProtocolMessage.getMessageData().getTrumpCard();
        Card trumpCard = new Card(cardData.getRank(), cardData.getSuit());

        //find trump card node
        CardNode trumpCardNode = mCardNodes.get(trumpCard);

        //stock pile layout parameters
        float stockPileXPosition = (getSceneSize().getX() - mCardWidth) / 2;
        float stockPileYPosition = 0;
        float stockPileScaleSize = 0.7f;

        //init "field piles" ( can be no more than 2 cards)
        for (int i = (PLAYER_TO_THE_LEFT_PILE_INDEX + 1); i < CARDS_COUNT / 2; i++) {
            mPileIndexToCardListMap.put(i, new ArrayList<Card>(2));
        }

        //stock pile
        layoutPile(STOCK_PILE_INDEX, stockPileXPosition, stockPileYPosition, 100, stockPileScaleSize);

        //layout trump card separately
        layoutCard(stockPileXPosition, stockPileYPosition + mCardHeight / 4, 5, stockPileScaleSize, trumpCardNode);
        trumpCardNode.setSortingLayer(0);
        trumpCardNode.useFrontTextureRegion();

        //discard pile (off the screen)
        layoutPile(DISCARD_PILE_INDEX, -getSceneSize().getX(), getSceneSize().getY() / 2, 90, 1f);

        //player one pile (bottom middle)
        layoutPile(CURRENT_PLAYER_PILE_INDEX, (getSceneSize().getX() - mCardWidth) / 2, getSceneSize().getY() - mCardHeight, 90, 1f);

        //player two pile (top right)
        layoutPile(PLAYER_TO_THE_RIGHT_PILE_INDEX, (getSceneSize().getX() - mCardWidth), 0, 90, 1f);

        //player three pile (top left)
        layoutPile(PLAYER_TO_THE_LEFT_PILE_INDEX, 0, 0, 90, 1f);

        float leftBorderX = getSceneSize().getX() * 0.3f;
        float rightBorderX = getSceneSize().getX() * 0.7f;

        float leftBorderY = getSceneSize().getY() * 0.3f;
        float rightBorderY = getSceneSize().getY() * 0.5f;

        //init "field piles" positions
        for (int i = (PLAYER_TO_THE_LEFT_PILE_INDEX + 1); i < CARDS_COUNT / 2; i++) {
            float x = YANMathUtils.randomInRange(leftBorderX, rightBorderX);
            float y = YANMathUtils.randomInRange(leftBorderY, rightBorderY);
            mPileIndexToPositionMap.put(i, new YANVector2(x, y));
        }
    }

    private void handleRequestRetaliatePilesMessage(RequestRetaliatePilesMessage requestRetaliatePilesMessage) {
        mRequestedRetaliation = true;
    }

    private void handleRequestCardForAttackMessage(RequestCardForAttackMessage requestCardForAttackMessage) {
        mCardForAttackRequested = true;
    }

    private void handleCardMoveMessage(CardMovedProtocolMessage cardMovedMessage) {

        //extract data
        Card movedCard = new Card(cardMovedMessage.getMessageData().getMovedCard().getRank(), cardMovedMessage.getMessageData().getMovedCard().getSuit());
        int fromPile = cardMovedMessage.getMessageData().getFromPileIndex();
        int toPile = cardMovedMessage.getMessageData().getToPileIndex();

        //execute the move
        moveCardFromPileToPile(movedCard, fromPile, toPile);

        //check if card goes to or from player 1 pile
        if (toPile == CURRENT_PLAYER_PILE_INDEX || fromPile == CURRENT_PLAYER_PILE_INDEX) {

            if (toPile == CURRENT_PLAYER_PILE_INDEX) {
                mPlayerOneCardNodes.add(mCardNodes.get(movedCard));
            } else {
                mPlayerOneCardNodes.remove(mCardNodes.get(movedCard));
            }

            layoutPlayerOneCards();
        }

        //player 2
        else if (toPile == PLAYER_TO_THE_RIGHT_PILE_INDEX || fromPile == PLAYER_TO_THE_RIGHT_PILE_INDEX) {
            if (toPile == PLAYER_TO_THE_RIGHT_PILE_INDEX) {
                mPlayerTwoTextureNodeCards.add(mCardNodes.get(movedCard));
            } else {
                mPlayerTwoTextureNodeCards.remove(mCardNodes.get(movedCard));
            }


            //TODO : cache slots
            List<CardsLayouterSlotImpl> slots = new ArrayList<>(mPlayerTwoTextureNodeCards.size());
            for (int i = 0; i < mPlayerTwoTextureNodeCards.size(); i++) {
                slots.add(new CardsLayouterSlotImpl());
            }

            //layout the slots
            mThreePointFanLayouterPlayerTwo.layoutRowOfSlots(slots);

            //make the layouting
            for (int i = 0; i < slots.size(); i++) {
                CardsLayouterSlotImpl slot = slots.get(i);
                YANTexturedNode node = mPlayerTwoTextureNodeCards.get(i);
                node.setSortingLayer(slot.getSortingLayer());
                //make the animation
                mCardsTweenAnimator.animateCardToValues(node, slot.getPosition().getX(), slot.getPosition().getY(), slot.getRotation(), null);
                mCardsTweenAnimator.animateSize(node, mCardWidth * CARD_SCALE_AMOUNT_OPPONENT, mCardHeight * CARD_SCALE_AMOUNT_OPPONENT, 0.5f);
            }
        }

        //player 3
        else if (toPile == PLAYER_TO_THE_LEFT_PILE_INDEX || fromPile == PLAYER_TO_THE_LEFT_PILE_INDEX) {
            if (toPile == PLAYER_TO_THE_LEFT_PILE_INDEX) {
                mPlayerThreeTextureNodeCards.add(mCardNodes.get(movedCard));
            } else {
                mPlayerThreeTextureNodeCards.remove(mCardNodes.get(movedCard));
            }

            List<CardsLayouterSlotImpl> slots = new ArrayList<>(mPlayerThreeTextureNodeCards.size());
            for (int i = 0; i < mPlayerThreeTextureNodeCards.size(); i++) {
                slots.add(new CardsLayouterSlotImpl());
            }

            //layout the slots
            mThreePointFanLayouterPlayerThree.layoutRowOfSlots(slots);

            //make the layouting
            for (int i = 0; i < slots.size(); i++) {
                CardsLayouterSlotImpl slot = slots.get(i);
                YANTexturedNode node = mPlayerThreeTextureNodeCards.get(i);
                node.setSortingLayer(slot.getSortingLayer());
                //make the animation
                mCardsTweenAnimator.animateCardToValues(node, slot.getPosition().getX(), slot.getPosition().getY(), slot.getRotation(), null);
                mCardsTweenAnimator.animateSize(node, mCardWidth * CARD_SCALE_AMOUNT_OPPONENT, mCardHeight * CARD_SCALE_AMOUNT_OPPONENT, 0.5f);
            }
        } else {
            //set the sorting layer higher
            mTopCardOnFieldSortingLayer++;
            mCardNodes.get(movedCard).setSortingLayer(mTopCardOnFieldSortingLayer);
        }
    }

    private void layoutPlayerOneCards() {
        //update layouter to recalculate positions
        mPlayerCardsLayouter.setActiveSlotsAmount(mPlayerOneCardNodes.size());

        //each index in nodes array corresponds to slot index
        for (int i = 0; i < mPlayerOneCardNodes.size(); i++) {
            CardNode card = mPlayerOneCardNodes.get(i);
            CardsLayoutSlot slot = mPlayerCardsLayouter.getSlotAtPosition(i);
            card.setSortingLayer(slot.getSortingLayer());
            mCardsTweenAnimator.animateCardToSlot(card, slot);
        }
    }

    private void moveCardFromPileToPile(Card movedCard, int fromPile, int toPile) {

        //remove card entity from its current pile
        mPileIndexToCardListMap.get(fromPile).remove(movedCard);

        //add card entity to its new pile
        mPileIndexToCardListMap.get(toPile).add(movedCard);

        //find node
        CardNode cardNode = mCardNodes.get(movedCard);

        //find destination position
        float destX = mPileIndexToPositionMap.get(toPile).getX();
        float destY = mPileIndexToPositionMap.get(toPile).getY();
        float destRotation = YANMathUtils.randomInRange(-70, 70);

        //make the animation
        mCardsTweenAnimator.animateCardToValues(cardNode, destX, destY, destRotation, null);
        mCardsTweenAnimator.animateSize(cardNode, mCardWidth, mCardHeight, 0.5f);

        if (fromPile == CURRENT_PLAYER_PILE_INDEX || toPile == CURRENT_PLAYER_PILE_INDEX || toPile > PLAYER_TO_THE_LEFT_PILE_INDEX || toPile == DISCARD_PILE_INDEX) {


            //show the card
            cardNode.useFrontTextureRegion();
        } else {
            //hide the card
            cardNode.useBackTextureRegion();
        }

        if (toPile > PLAYER_TO_THE_LEFT_PILE_INDEX) {
            //moving to field pile
            //we need to adjust sorting layer
            int sortingLayer = (mPileIndexToCardListMap.get(toPile).size() == 1) ? 1 : 2;
            cardNode.setSortingLayer(sortingLayer);
        }

        if (fromPile > PLAYER_TO_THE_LEFT_PILE_INDEX) {
            //we need to adjust sorting layer
            int sortingLayer = (mPileIndexToCardListMap.get(fromPile).size() > 0) ? 2 : 1;
            cardNode.setSortingLayer(sortingLayer);
        }


    }

}