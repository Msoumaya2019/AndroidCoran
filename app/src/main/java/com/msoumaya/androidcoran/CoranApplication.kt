package com.msoumaya.androidcoran

import android.app.Application
import com.msoumaya.androidcoran.data.Repository

class CoranApplication: Application() { val repository by lazy { Repository(this) } }
