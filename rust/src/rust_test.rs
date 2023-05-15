use ndarray::{array, Array1};

#[derive(Debug, Clone, PartialEq)]
pub struct Foo {
    coords: Array1<i32>,
}

impl Foo {
    pub fn new(coords: Array1<i32>) -> Self {
        Foo { coords }
    }
}

impl From<Vec<i32>> for Foo {
    fn from(coords: Vec<i32>) -> Self {
        Foo::new(coords.into())
    }
}

impl From<Array1<i32>> for Foo {
    fn from(coords: Array1<i32>) -> Self {
        Foo::new(coords)
    }
}

impl Foo {
    fn add<T: Into<Self>>(
        &self,
        other: T,
    ) -> Self {
        let p: Foo = other.into();
        Foo::new(&self.coords + p.coords)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_from() {
        assert_eq!(Foo::from(vec![1, 2, 3]), Foo::new(array![1, 2, 3]));
    }

    #[test]
    fn test_add() {
        let a = Foo::from(vec![1, 2, 3]);
        let b = Foo::from(vec![4, 5, 6]);

        assert_eq!(a.add(b), Foo::from(vec![5, 7, 9]));
    }
}
