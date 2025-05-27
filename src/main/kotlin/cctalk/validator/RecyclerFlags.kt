package cctalk.validator

import cctalk.validator.ValidatorBillPosition.Unknown

enum class RecyclerFlags(val code: Long) {
    /// <summary>No flags set.</summary>
    None(0x0000),
    /// <summary>Recycler not connected.</summary>
    NotConnected(0x0001),
    /// <summary>Recycler full.</summary>
    Full(0x0002),
    /// <summary>Recycler empty.</summary>
    Empty(0x0004),
    /// <summary>Recycler door open.</summary>
    DoorOpen(0x0008),
    /// <summary>Unit disabled.</summary>
    Disabled(0x0010),
    /// <summary>A recycler error occured.</summary>
    RecyclerError(0x0020),
    /// <summary>Entrance bill remain.</summary>
    BillRemain(0x0040),
    /// <summary>An Acceptor error occured.</summary>
    AcceptorError(0x0080),
    /// <summary>Recycler is operating.</summary>
    Busy(0x0100),
    /// <summary>Bill jam in the recycler.</summary>
    RecyclerJam(0x0200),
    /// <summary>Recycler motor malfunction.</summary>
    MotorError(0x0400),
    /// <summary>error is detected during note dispensing.</summary>
    PayoutError(0x0800),
    /// <summary>The recycler box is not in place</summary>
    RecycleBoxOpen(0x1000),
    /// <summary>Abnormal recycler condition.</summary>
    HardwareError(0x2000),
    /// <summary>Normal recycler condition.</summary>
    Normal(0x4000);

    companion object {
        fun fromCode(code: Long): RecyclerFlags = RecyclerFlags.entries.firstOrNull { it.code == code } ?: None
    }
}