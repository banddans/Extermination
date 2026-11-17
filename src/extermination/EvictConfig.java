package extermination;

import arc.math.*;

import mindustry.game.*;
import mindustry.gen.*;
import mindustry.content.*;

public class EvictConfig {
  // GAME CONFIG

  public final static int serpulo = 1;
  public final static int erekir = 2;
  public int planet = erekir;

  // HEX GRID CHARACTERISTICS, defaults supplied

  public int mapSizeTiles = 705;
  public int mapTileCenter = generateMapTileCenter(mapSizeTiles);
  public int hexLayers = 2;

  public int parcelDiameter = 78;
  public int parcelRadius = generateParcelRadius(parcelDiameter);
  public int parcelRsquare = generateParcelRsquare(parcelRadius);

  // offset of parcels all in the same horizontal row
  public int parcelHorizOffset = generateParcelHorizOffset(parcelDiameter);

  // offset of parcels that are in a higher or lower row
  public int parcelVertOffsetX = generateParcelVertOffsetX(parcelHorizOffset);
  public int parcelVertOffsetY = generateParcelVertOffsetY(parcelDiameter);

  public static Rand rand = new Rand();

  // doorway creation
  public Boolean generateBorders = true;

  public double borderOpenRoll = 8;
  public double borderDoorRoll = 8;
  public double borderFenceRoll = 2;
  public double borderWallRoll = 1;

  // map generation background blocks
  // serpulo
  public int mapFloorID = Blocks.darksand.id;
  public int mapOverlayID = Blocks.air.id;
  public int mapWallID = Blocks.dirtWall.id;

  // SCHEMATICS

  public Schematic fallenShard = Schematics.readBase64(
      "bXNjaAF4nD2SXW6DMBAG1/849BC9QK7RC/SxygNNLDUSAQSVev16d5o+oMHst4O9IKOMQeIyPZq8vE3z3JbX969pv8l4a8d1v2/f93URkTxPn20+xH9coozXddvafv7pDTIe6zzt521a2iz50ZZb2+V0Xfd2PswkcumXOAkKDwIoYABV4USIO1YeBBBBBqVXOwZtcr3dVieFx+JVoA4PAjWzeEmgy7RWxClM5pF5ZAFZ0G04c3j/PEFAFpAFlWmtkDRZQBaQRU4Y2eJ/c2JVwABsIIlk4n2JSCKSkWUimUgmkp8RsxTOUBiVDi54G7/Th5FIApla0ZkVLKVbLGlnGJD9zX1QmbOPGPRhBAlkaoXkQF8FJqvIKkepfPD6/CMiqwxs+5UXV1oqlpP9MXZvCKCAAfSGXwijIgg=");

  public Schematic serpPlayerStart = Schematics.readBase64(
      "bXNjaAF4nDWTXa6bMBBGBxuMwTZ3C91A9tL3qg++hEqRKCBIGmUn3Wtvm87n7yZRcpQwc2bGP/Imb1bqJf+cJH6d82Pavxxbvi8SztMx7pftelkXEXFzfp/mQ8y3772Ecd22aT/d8zyLzfsoflyXX9Nj3SUc65z305aXaZb2GPP1Ou3Svhc+pN/Wu2Yu63kSf1vmNZ/1cRzXfTott3GeboeEfNlPP/J4XTVB5Ld+pNK3vgxhiSDlFYmEP6sSy3B8GSZb/q6lgqGhyBEtQzxDOqInglSwRIYkYsCzT3FpyCoMYcUCtVj8VQoZFjIohGeeIR3RE4GIYuFMnHXgrPIa2dgigB1vVCsVLCtYVrCo0Cg6MYjsmRdhslRbqms6azjRc0M4QmVO4b32/vxAVNXqd6dR3iKrGGsaG6oatNEIqptWUSNY3y1sWI8ebTcYF4hEgsjR4GAAtGeHBrx5/tMqKI+VLAJHgaPAQYDlrbC9LfYEsERN9AwJRCRKnmeeZ55nnkeeU/Ra/0M70JEqrGzZKM/ZPWY35chUgCEsoYJW0WNBOs0z2JGS13HNep6dHhPXiBOLPpuyZpjU+Qq1K5wSHQvwhNaCMpTjbqFKrwthgCIO3IyAXcEvh2kDTYGmULpWfKYPr/tkcOiLJXJDIo92xOEDGqI4I52Rzggn0hNlAy9nuZe4owYXyfBPHExFTTREcSY6E52JfaaySoqAe5LQKTY/6Tr9ef4t/nI5B978gddz4PUfkAVEQnv5D45DTdM=");

  private static String dataLocation = System.getProperty("user.home") + "/eviction";
  private static String erekirDataLocation = dataLocation + "/erekir";
  private static String serpuloDataLocation = dataLocation + "/serpulo";

  public static String dataFile(String file) {
    return dataLocation + "/" + file;
  }

  public static String serpuloDataFile(String file) {
    return serpuloDataLocation + "/" + file;
  }

  public static String erekirDataFile(String file) {
    return erekirDataLocation + "/" + file;
  }

  // map detail validation helpers
  //
  private static int generateMapTileCenter(int mapSize) {
    return (int) Math.round(mapSize / 2.0);
  }

  private static int generateParcelRadius(int diameter) {
    return (int) Math.round(diameter / 2.0);
  }

  private static int generateParcelRsquare(int radius) {
    return radius * radius;
  }

  private static int generateParcelHorizOffset(int diameter) {
    return (int) Math.round(diameter * 0.9487);
  }

  private static int generateParcelVertOffsetX(int horizoffset) {
    return (int) Math.round(horizoffset / 2.0);
  }

  private static int generateParcelVertOffsetY(int diameter) {
    return (int) Math.round(diameter * 0.8205);
  }

  // core explosion unit survival rates
  // TODO make these configurable instead of static
  public float unitSurvival(Unit u) {
    float output = 0.5f;

    if (u.type == UnitTypes.antumbra) {
      output = 0.25f;
    }
    if (u.type == UnitTypes.vela) {
      output = 0.25f;
    }
    if (u.type == UnitTypes.scepter) {
      output = 0.25f;
    }
    if (u.type == UnitTypes.sei) {
      output = 0.25f;
    }
    if (u.type == UnitTypes.quad) {
      output = 0.25f;
    }
    if (u.type == UnitTypes.arkyid) {
      output = 0.25f;
    }

    if (u.type == UnitTypes.toxopid) {
      output = 0.125f;
    }
    if (u.type == UnitTypes.eclipse) {
      output = 0.125f;
    }
    if (u.type == UnitTypes.oct) {
      output = 0.125f;
    }
    if (u.type == UnitTypes.omura) {
      output = 0.125f;
    }
    if (u.type == UnitTypes.corvus) {
      output = 0.125f;
    }
    if (u.type == UnitTypes.reign) {
      output = 0.125f;
    }

    if (u.type == UnitTypes.vanquish) {
      output = 0.25f;
    }
    if (u.type == UnitTypes.tecta) {
      output = 0.25f;
    }
    if (u.type == UnitTypes.quell) {
      output = 0.25f;
    }

    if (u.type == UnitTypes.disrupt) {
      output = 0.125f;
    }
    if (u.type == UnitTypes.collaris) {
      output = 0.125f;
    }
    if (u.type == UnitTypes.conquer) {
      output = 0.125f;
    }

    return output;
  }

}
