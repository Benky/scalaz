package scalaz

sealed trait MA[M[_], A] {
  val v: M[A]

  def map[B](f: A => B)(implicit t: Functor[M]) = t.fmap(v, f)

  def |>[B](f: A => B)(implicit t: Functor[M]) = map(f)

  def <|:[B](f: A => B)(implicit t: Functor[M]) = map(f)

  def |>-[B](f: => B)(implicit t: Functor[M]) = map(_ => f)

  def -<|:[B](f: => B)(implicit t: Functor[M]) = |>-(f)

  def <*>[B](f: M[A => B])(implicit a: Apply[M]) = a(f, v)

  def <*>:[B](f: M[A => B])(implicit a: Apply[M]) = <*>(f)

  def *>[B](k: M[B])(implicit f: Functor[M], a: Apply[M]) = a(f.fmap(v, (_: A) => (b: B) => b), k)

  def <*[B](k: M[B])(implicit f: Functor[M], a: Apply[M]) = a(f.fmap(v, (a: A) => (_: B) => a), k)

  def <**>[B](k: M[B])(implicit f: Functor[M], a: Apply[M]) = a(f.fmap(v, (a: A) => (b: B) => (a, b)), k)

  def liftA[B, C](b: M[B], z: A => B => C)(implicit f: Functor[M], a: Apply[M]) = a(f.fmap(v, z), b)

  def liftA[B, C, D](b: M[B], c: M[C], z: A => B => C => D)(implicit f: Functor[M], a: Apply[M]) =
    a(a(f.fmap(v, z), b), c)

  def liftA[B, C, D, E](b: M[B], c: M[C], d: M[D], z: A => B => C => D => E)(implicit f: Functor[M], a: Apply[M]) =
    a(a(a(f.fmap(v, z), b), c), d)

  def liftA[B, C, D, E, F](b: M[B], c: M[C], d: M[D], e: M[E], z: A => B => C => D =>
 E => F)(implicit f: Functor[M], a: Apply[M]) =
    a(a(a(a(f.fmap(v, z), b), c), d), e)

  def <<*>>[B](b: M[B])(implicit f: Functor[M], a: Apply[M]) = liftA(b, a => (b: B) => (a, b))

  def <<*>>[B, C](b: M[B], c: M[C])(implicit f: Functor[M], a: Apply[M]) = liftA(b, c, a => (b: B) => (c: C) => (a, b, c))

  def <<*>>[B, C, D](b: M[B], c: M[C], d: M[D])(implicit f: Functor[M], a: Apply[M]) = liftA(b, c, d, a => (b: B) => (c: C)
 => (d: D) => (a, b, c, d))

  def <<*>>[B, C, D, E](b: M[B], c: M[C], d: M[D], e: M[E])(implicit f: Functor[M], a: Apply[M]) = liftA(b, c, d, e, a =>
(b: B) => (c: C) => (d: D) => (e: E) => (a, b, c, d, e))

  def >>=[B](f: A => M[B])(implicit b: Bind[M]) = b.bind(v, f)

  def flatMap[B](f: A => M[B])(implicit b: Bind[M]) = >>=(f)

  def >->[B](f: => M[B])(implicit b: Bind[M]) = >>=(_ => f)

  def <+>(z: => M[A])(implicit p: Plus[M]) = p.plus(v, z)

  def ::+::(a: A)(implicit p: Plus[M], q: Pure[M]) = p.plus(q.pure(a), v)

  def <|[B](f: B => A)(implicit t: Cofunctor[M]) = t.comap(v, f)

  def <|:[B](f: B => A)(implicit t: Cofunctor[M]) = <|(f)

  def -<|[B](f: => A)(implicit t: Cofunctor[M]) = <|((_: B) => f)

  def |>-:[B](f: => A)(implicit t: Cofunctor[M]) = -<|[B](f)

  def foreach(f: A => Unit)(implicit e: Each[M]) = e.each(v, f)

  def ->>(f: A => Unit)(implicit e: Each[M]) = foreach(f)

  def foldl[B](b: B, f: (B, A) => B)(implicit r: FoldLeft[M]) = r.foldLeft[B, A](v, b, f)

  def foldl1(f: (A, A) => A)(implicit r: FoldLeft[M]) = foldl[Option[A]](None, (a1, a2) => Some(a1 match {
    case None => a2
    case Some(x) => f(a2, x)
  })) getOrElse (error("foldl1 on empty"))

  def listl(implicit r: FoldLeft[M]) = {
    val b = new scala.collection.mutable.ListBuffer[A]
    foldl[scala.Unit]((), (x, a) => b += a)
    b.toList
  }

  def suml(implicit r: FoldLeft[M], m: Monoid[A]) = foldl[A](m.zero, m append (_, _))

  def items(implicit r: FoldLeft[M]) = foldl[Int](0, (b, _) => b + 1)

  def max(implicit r: FoldLeft[M], ord: Order[A]) =
    foldl1((x: A, y: A) => if(ord.order(x, y) == GT) x else y)

  def min(implicit r: FoldLeft[M], ord: Order[A]) =
    foldl1((x: A, y: A) => if(ord.order(x, y) == LT) x else y)

  def ->-(f: A => Digit)(implicit t: FoldLeft[M]) =
    foldl[Long](0L, (n, a) => n * 10L + f(a))

  def ->=(f: A => Char)(implicit t: Functor[M]): M[Option[Digit]] = {
    import CharW._
    t.fmap(v, f andThen (_.digit))    
  }

  def =>=(f: A => Char)(implicit t: Traverse[M]): Option[M[Digit]] = {
    import CharW._
    t.traverse[Option, Char, Digit](_.digit,  t.fmap(v, f))
  }

  def foldr[B](b: B, f: (A, => B) => B)(implicit r: FoldRight[M]) = r.foldRight(v, b, f)

  def foldr1(f: (A, => A) => A)(implicit r: FoldRight[M]) = foldr[Option[A]](None, (a1, a2) => Some(a2 match {
    case None => a1
    case Some(x) => f(a1, x)
  })) getOrElse (error("foldr1 on empty"))

  def sumr(implicit r: FoldRight[M], m: Monoid[A]) = foldr[A](m.zero, m append (_, _))

  def listr(implicit r: FoldRight[M]) = foldr[List[A]](Nil, _ :: _)

  def stream(implicit r: FoldRight[M]) = foldr[Stream[A]](Stream.empty, Stream.cons(_, _))

  def !(n: Int)(implicit r: FoldRight[M]) = stream(r)(n)

  def any(p: A => Boolean)(implicit r: FoldRight[M]) = foldr[Boolean](false, p(_) || _)

  def all(p: A => Boolean)(implicit r: FoldRight[M]) = foldr[Boolean](true, p(_) && _)

  def nil(implicit r: FoldRight[M]) = all(_ => false)

  def empty(implicit r: FoldRight[M]) = foldr[Boolean](true, (_, _) => false)   

  def splitWith(p: A => Boolean)(implicit r: FoldRight[M]) = foldr[(List[List[A]], Option[Boolean])]((Nil, None), (
a, b) => {
      val pa = p(a)
      (b match {
        case (_, None) => List(List(a))
        case (x, Some(q)) => if(pa == q) (a :: x.head) :: x.tail else List(a) :: x
      }, Some(pa))
    })._1

  def selectSplit(p: A => Boolean)(implicit r: FoldRight[M]) = foldr[(List[List[A]], Boolean)]((Nil, false), (a, xb
) => xb match {
      case (x, b) => {
        val pa = p(a)
        (if(pa) if(b) (a :: x.head) :: x.tail else List(a) :: x else x, pa)
      }
    })._1

  def para[B](b: B, f: (=> A, => M[A], B) => B)(implicit p: Paramorphism[M]) = p.para(v, b, f)

  trait TraverseM[F[_]] {
    def apply[B](f: A => F[B])(implicit a: Applicative[F]): F[M[B]]
  }

  def traverse[F[_]](implicit t: Traverse[M]) = new TraverseM[F] {
    def apply[B](f: A => F[B])(implicit a: Applicative[F]) = t.traverse[F, A, B](f, v)
  }

  def ==>>[B](f: A => B)(implicit t: Traverse[M], m: Monoid[B]): B = {
    case class Acc[B, A](acc: B)

    implicit val AccApply = new Apply[PartialApply1Of2[Acc, B]#Apply] {
      def apply[A, X](f: Acc[B, A => X], fa: Acc[B, A]) = Acc[B, X](m append (f.acc, fa.acc))
    }

    implicit val AccPure = new Pure[PartialApply1Of2[Acc, B]#Apply] {
      def pure[A](a: A) = Acc[B, A](m.zero)
    }

    implicit val AccApplicative = Applicative.applicative[PartialApply1Of2[Acc, B]#Apply]

    traverse[PartialApply1Of2[Acc, B]#Apply](t)(a => Acc[B, B](f(a))).acc
  }

  def -->>(implicit t: Traverse[M], m: Monoid[A]) = ==>>(identity[A])

  def cojoin(implicit j: Cojoin[M]) = j.cojoin(v)

  def =>>[B](f: M[A] => B)(implicit w: Comonad[M]) = w.cobind(v, f)

  def copure[B](implicit p: Copure[M]) = p.copure(v)
}

object MA {
  def ma[M[_]] = new PartialWrapMA[M, MA] {
    def apply[A](a: M[A]) = new MA[M, A] {
      val v = a
    }
  }

  implicit def IdentityMA[A](a: Identity[A]): MA[Identity, A] = ma[Identity](a)

  implicit def ContinuationMA[R, A](a: Continuation[R, A]) = ma[PartialApply1Of2[Continuation, R]#Apply](a)

  implicit def NonEmptyListMA[A](a: NonEmptyList[A]) = ma[NonEmptyList](a)

  implicit def StateMA[S, A](a: State[S, A]) = ma[PartialApply1Of2[State, S]#Apply](a)

  implicit def EqualMA[A](a: Equal[A]) = ma[Equal](a)

  implicit def OrderMA[A](a: Order[A]) = ma[Order](a)

  implicit def ShowMA[A](a: Show[A]) = ma[Show](a)

  implicit def Tuple1MA[A](a: Tuple1[A]) = ma[Tuple1](a)

  implicit def Tuple2MA[R, A](a: (R, A)) = ma[PartialApply1Of2[Tuple2, R]#Apply](a)

  implicit def Tuple3MA[R, S, A](a: (R, S, A)) = ma[PartialApply2Of3[Tuple3, R, S]#Apply](a)

  implicit def Tuple4MA[R, S, T, A](a: (R, S, T, A)) = ma[PartialApply3Of4[Tuple4, R, S, T]#Apply](a)

  implicit def Tuple5MA[R, S, T, U, A](a: (R, S, T, U, A)) = ma[PartialApply4Of5[Tuple5, R, S, T, U]#Apply](a)

  implicit def Tuple6MA[R, S, T, U, V, A](a: (R, S, T, U, V, A)) = ma[PartialApply5Of6[Tuple6, R, S, T, U, V]#Apply](a)

  implicit def Tuple7MA[R, S, T, U, V, W, A](a: (R, S, T, U, V, W, A)) = ma[PartialApply6Of7[Tuple7, R, S, T, U, V, W]#Apply](a)

  implicit def Function0MA[A](a: Function0[A]) = ma[Function0](a)

  implicit def Function1MA[R, A](a: R => A) = ma[PartialApply1Of2[Function1, R]#Apply](a)

  implicit def Function2MA[R, S, A](a: (R, S) => A) = ma[PartialApply2Of3[Function2, R, S]#Apply](a)

  implicit def Function3MA[R, S, T, A](a: (R, S, T) => A) = ma[PartialApply3Of4[Function3, R, S, T]#Apply](a)

  implicit def Function4MA[R, S, T, U, A](a: (R, S, T, U) => A) = ma[PartialApply4Of5[Function4, R, S, T, U]#Apply](a)

  implicit def Function5MA[R, S, T, U, V, A](a: (R, S, T, U, V) => A) = ma[PartialApply5Of6[Function5, R, S, T, U, V]#Apply](a)

  implicit def Function6MA[R, S, T, U, V, W, A](a: (R, S, T, U, V, W) => A) = ma[PartialApply6Of7[Function6, R, S, T, U, V, W]#Apply](a)

  implicit def ListMA[A](a: List[A]) = ma[List](a)

  implicit def StreamMA[A](a: Stream[A]) = ma[Stream](a)

  implicit def OptionMA[A](a: Option[A]) = ma[Option](a)

  implicit def ArrayMA[A](a: Array[A]) = ma[Array](a)

  implicit def EitherLeftMA[X, A](a: Either.LeftProjection[A, X]) = ma[PartialApply1Of2[Either.LeftProjection, X]#Flip](a)

  implicit def EitherRightMA[X, A](a: Either.RightProjection[X, A]) = ma[PartialApply1Of2[Either.RightProjection, X]#Apply](a)

  implicit def ZipperMA[A](a: Zipper[A]) = ma[Zipper](a)

  import java.util._
  import java.util.concurrent._

  implicit def ArrayListMA[A](a: ArrayList[A]) = ma[ArrayList](a)

  implicit def HashSetMA[A](a: HashSet[A]) = ma[HashSet](a)

  implicit def LinkedHashSetMA[A](a: LinkedHashSet[A]) = ma[LinkedHashSet](a)

  implicit def LinkedListMA[A](a: LinkedList[A]) = ma[LinkedList](a)

  implicit def PriorityQueueMA[A](a: PriorityQueue[A]) = ma[PriorityQueue](a)

  implicit def StackMA[A](a: Stack[A]) = ma[Stack](a)

  implicit def TreeSetMA[A](a: TreeSet[A]) = ma[TreeSet](a)

  implicit def VectorMA[A](a: Vector[A]) = ma[Vector](a)

  implicit def ArrayBlockingQueueMA[A](a: ArrayBlockingQueue[A]) = ma[ArrayBlockingQueue](a)

  implicit def ConcurrentLinkedQueueMA[A](a: ConcurrentLinkedQueue[A]) = ma[ConcurrentLinkedQueue](a)

  implicit def CopyOnWriteArrayListMA[A](a: CopyOnWriteArrayList[A]) = ma[CopyOnWriteArrayList](a)

  implicit def CopyOnWriteArraySetMA[A](a: CopyOnWriteArraySet[A]) = ma[CopyOnWriteArraySet](a)

  implicit def LinkedBlockingQueueMA[A](a: LinkedBlockingQueue[A]) = ma[LinkedBlockingQueue](a)

  implicit def PriorityBlockingQueueMA[A](a: PriorityBlockingQueue[A]) = ma[PriorityBlockingQueue](a)

  implicit def SynchronousQueueMA[A](a: SynchronousQueue[A]) = ma[SynchronousQueue](a)

}